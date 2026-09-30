#!/usr/bin/env python3
"""스켈레톤을 부팅해 springdoc이 내는 /v3/api-docs 를 파일로 저장한다.

게이트의 스펙 추출기다. 구현이 자기 계약을 무엇이라고 말하는지는 AI가 쓴 설명이 아니라 부팅한
애플리케이션에서 뽑는다. 그래서 이 스크립트는 실패를 조용히 통과시키지 않는다. 컴파일·부팅·조회 중
어디서 막혔는지 stderr에 적고 0이 아닌 코드로 끝낸다.

사용법:
    python tools/dump_api_docs.py --skeleton <dir> --out <path> [--port 0]

종료 코드:
    0  api-docs 를 받아 저장했다
    2  사용법이나 환경이 잘못됐다. 스켈레톤 경로, JDK 버전, 메이븐 없음
    3  컴파일·패키징이 실패했다
    4  부팅이 실패했거나 제한 시간 안에 포트를 열지 못했다
    5  부팅은 했지만 api-docs 를 받지 못했다
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path
from queue import Empty, Queue

EXIT_ENVIRONMENT = 2
EXIT_BUILD = 3
EXIT_BOOT = 4
EXIT_FETCH = 5

MIN_JAVA_MAJOR = 17
LOG_TAIL_LINES = 60

# 윈도우 콘솔의 기본 인코딩은 UTF-8이 아니라서 실패 이유가 깨져 읽히거나 인코딩 오류로 다시 죽는다.
# 실패 이유를 그대로 되먹여야 하므로 출력 인코딩을 고정한다.
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace")

# Spring Boot 3.3 은 "Tomcat started on port 51234 (http) with context path '/'" 로 적는다.
# 이전 판본의 "Tomcat started on port(s): 8080 (http)" 도 함께 받는다.
PORT_PATTERNS = (
    re.compile(r"Tomcat started on port\(s\):\s*(\d+)"),
    re.compile(r"Tomcat started on port\s*:?\s*(\d+)"),
    re.compile(r"Tomcat initialized with port\(s\):\s*(\d+)"),
    re.compile(r"Tomcat initialized with port\s*:?\s*(\d+)"),
)


def fail(exit_code, reason, log_lines=None):
    print("dump_api_docs: " + reason, file=sys.stderr)
    if log_lines:
        print("--- 마지막 로그 ---", file=sys.stderr)
        for line in log_lines[-LOG_TAIL_LINES:]:
            print(line.rstrip(), file=sys.stderr)
        print("--- 로그 끝 ---", file=sys.stderr)
    sys.exit(exit_code)


def java_executable(java_home):
    return java_home / "bin" / ("java.exe" if os.name == "nt" else "java")


def java_major_version(java):
    result = subprocess.run([str(java), "-version"], capture_output=True, text=True,
                            errors="replace")
    text = (result.stderr or "") + (result.stdout or "")
    match = re.search(r'version "(\d+)(?:\.(\d+))?', text)
    if not match:
        return None
    first = int(match.group(1))
    # 1.8.0_x 처럼 적는 옛 표기에서는 두 번째 숫자가 주 버전이다.
    if first == 1 and match.group(2):
        return int(match.group(2))
    return first


def resolve_java_home(explicit):
    """빌드와 실행에 쓸 JDK를 정한다. JDK 17 아래면 멈춘다."""
    candidate = explicit or os.environ.get("JAVA_HOME")
    if not candidate:
        fail(EXIT_ENVIRONMENT, "JAVA_HOME 이 없다. --java-home 으로 JDK 17 이상을 가리켜라.")
    java_home = Path(candidate)
    java = java_executable(java_home)
    if not java.exists():
        fail(EXIT_ENVIRONMENT, str(java) + " 가 없다. --java-home 이 JDK 설치 경로인지 확인하라.")
    major = java_major_version(java)
    if major is None:
        fail(EXIT_ENVIRONMENT, str(java) + " 의 버전을 읽지 못했다.")
    if major < MIN_JAVA_MAJOR:
        fail(EXIT_ENVIRONMENT,
             "JDK {0} 으로는 이 스켈레톤을 빌드할 수 없다. JDK {1} 이상을 JAVA_HOME 이나 "
             "--java-home 으로 지정하라.".format(major, MIN_JAVA_MAJOR))
    return java_home


def resolve_maven(skeleton):
    """메이븐 실행 명령을 정한다. 스켈레톤의 래퍼를 먼저 쓴다."""
    wrapper = skeleton / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    if wrapper.exists():
        return [str(wrapper)]
    on_path = shutil.which("mvn")
    if on_path:
        return [on_path]
    fail(EXIT_ENVIRONMENT,
         str(skeleton) + " 에 메이븐 래퍼가 없고 PATH 에도 mvn 이 없다.")
    raise AssertionError("unreachable")


def find_jar(skeleton):
    target = skeleton / "target"
    jars = [p for p in target.glob("*.jar")
            if not p.name.endswith((".original", "-sources.jar", "-javadoc.jar"))]
    if not jars:
        fail(EXIT_BUILD, str(target) + " 에서 실행할 jar 를 찾지 못했다.")
    jars.sort(key=lambda p: p.stat().st_mtime, reverse=True)
    return jars[0]


def package(skeleton, maven, env, timeout):
    """테스트를 건너뛰고 실행 가능한 jar 를 만든다."""
    command = maven + ["-B", "-DskipTests", "package"]
    try:
        result = subprocess.run(command, cwd=str(skeleton), env=env, capture_output=True,
                                text=True, errors="replace", timeout=timeout)
    except subprocess.TimeoutExpired:
        fail(EXIT_BUILD, "패키징이 {0}초 안에 끝나지 않았다.".format(timeout))
        raise AssertionError("unreachable")
    if result.returncode != 0:
        lines = (result.stdout or "").splitlines() + (result.stderr or "").splitlines()
        fail(EXIT_BUILD,
             "패키징이 실패했다. exit {0}. 의존을 내려받지 못한 것이면 네트워크나 저장소 설정을 "
             "확인하라.".format(result.returncode),
             lines)
    return find_jar(skeleton)


class BootedApp:
    """부팅한 애플리케이션 프로세스와 그 로그를 들고 있는다."""

    def __init__(self, process, lines, queue):
        self.process = process
        self.lines = lines
        self.queue = queue

    def terminate(self):
        if self.process.poll() is not None:
            return
        self.process.terminate()
        try:
            self.process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait(timeout=20)


def boot(jar, java_home, port, env):
    """jar 를 직접 실행한다.

    메이븐으로 띄우지 않는 이유는 종료를 확실히 하기 위해서다. 메이븐이 자식 JVM을 따로 띄우면
    메이븐을 죽여도 애플리케이션이 남아 포트를 쥐고 있을 수 있다.
    """
    command = [str(java_executable(java_home)), "-jar", str(jar),
               "--server.port={0}".format(port), "--spring.main.banner-mode=off"]
    process = subprocess.Popen(command, cwd=str(jar.parent), env=env,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, errors="replace", bufsize=1)
    lines = []
    queue = Queue()

    def pump():
        for line in process.stdout:
            lines.append(line)
            queue.put(line)
        queue.put("")

    threading.Thread(target=pump, daemon=True).start()
    return BootedApp(process, lines, queue)


def wait_for_port(app, requested_port, timeout):
    """로그에서 실제로 열린 포트를 읽는다.

    포트를 지정해 띄웠으면 로그를 못 읽어도 그 값을 쓴다. 임의 포트로 띄웠으면 로그가 포트를 알아낼
    유일한 경로이므로 읽지 못하면 실패다.
    """
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if app.process.poll() is not None:
            fail(EXIT_BOOT,
                 "애플리케이션이 포트를 열기 전에 종료했다. exit {0}".format(app.process.returncode),
                 app.lines)
        try:
            line = app.queue.get(timeout=0.5)
        except Empty:
            continue
        for pattern in PORT_PATTERNS:
            match = pattern.search(line)
            if match:
                found = int(match.group(1))
                if found > 0:
                    return found
    if requested_port > 0:
        return requested_port
    fail(EXIT_BOOT,
         "{0}초 안에 열린 포트를 로그에서 읽지 못했다. 임의 포트로 띄웠으므로 포트를 알아낼 다른 "
         "방법이 없다.".format(timeout),
         app.lines)
    raise AssertionError("unreachable")


def wait_for_health(app, port, timeout):
    deadline = time.monotonic() + timeout
    last_error = "이유를 남기지 않았다"
    url = "http://127.0.0.1:{0}/actuator/health".format(port)
    while time.monotonic() < deadline:
        if app.process.poll() is not None:
            fail(EXIT_BOOT,
                 "애플리케이션이 준비되기 전에 종료했다. exit {0}".format(app.process.returncode),
                 app.lines)
        try:
            with urllib.request.urlopen(url, timeout=3) as response:
                if response.status == 200:
                    return
                last_error = "HTTP {0}".format(response.status)
        except (urllib.error.URLError, OSError) as error:
            last_error = str(error)
        time.sleep(0.5)
    fail(EXIT_BOOT,
         "{0}초 안에 {1} 가 응답하지 않았다. {2}".format(timeout, url, last_error),
         app.lines)


def fetch_api_docs(app, port, path, timeout):
    deadline = time.monotonic() + timeout
    last_error = "이유를 남기지 않았다"
    url = "http://127.0.0.1:{0}{1}".format(port, path)
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                body = response.read().decode("utf-8")
            return json.loads(body)
        except json.JSONDecodeError as error:
            fail(EXIT_FETCH, "{0} 이 JSON 이 아닌 것을 돌려줬다. {1}".format(url, error), app.lines)
        except urllib.error.HTTPError as error:
            last_error = "HTTP {0} {1}".format(error.code, error.reason)
        except (urllib.error.URLError, OSError) as error:
            last_error = str(error)
        time.sleep(1.0)
    fail(EXIT_FETCH,
         "{0} 에서 api-docs 를 받지 못했다. {1}".format(url, last_error),
         app.lines)
    raise AssertionError("unreachable")


def main():
    parser = argparse.ArgumentParser(description="스켈레톤을 부팅해 /v3/api-docs 를 저장한다.")
    parser.add_argument("--skeleton", required=True, help="스켈레톤 디렉터리. pom.xml 이 있는 곳")
    parser.add_argument("--out", required=True, help="api-docs JSON 을 저장할 경로")
    parser.add_argument("--port", type=int, default=0,
                        help="부팅에 쓸 포트. 0이면 임의 포트로 띄우고 로그에서 실제 포트를 읽는다")
    parser.add_argument("--api-docs-path", default="/v3/api-docs", help="api-docs 경로")
    parser.add_argument("--java-home", default=None,
                        help="빌드와 실행에 쓸 JDK 경로. 없으면 JAVA_HOME 을 쓴다")
    parser.add_argument("--build-timeout", type=int, default=900, help="패키징 제한 시간(초)")
    parser.add_argument("--boot-timeout", type=int, default=120, help="부팅 제한 시간(초)")
    parser.add_argument("--fetch-timeout", type=int, default=60, help="조회 제한 시간(초)")
    parser.add_argument("--skip-build", action="store_true",
                        help="이미 만들어 둔 jar 를 그대로 쓴다")
    parser.add_argument("--keep-log", default=None, help="애플리케이션 로그를 저장할 경로")
    args = parser.parse_args()

    skeleton = Path(args.skeleton).resolve()
    if not (skeleton / "pom.xml").exists():
        fail(EXIT_ENVIRONMENT, str(skeleton) + " 에 pom.xml 이 없다.")

    java_home = resolve_java_home(args.java_home)
    env = dict(os.environ)
    env["JAVA_HOME"] = str(java_home)

    if args.skip_build:
        jar = find_jar(skeleton)
    else:
        jar = package(skeleton, resolve_maven(skeleton), env, args.build_timeout)

    app = boot(jar, java_home, args.port, env)
    try:
        port = wait_for_port(app, args.port, args.boot_timeout)
        wait_for_health(app, port, args.boot_timeout)
        document = fetch_api_docs(app, port, args.api_docs_path, args.fetch_timeout)
    finally:
        app.terminate()
        if args.keep_log:
            log_path = Path(args.keep_log)
            log_path.parent.mkdir(parents=True, exist_ok=True)
            log_path.write_text("".join(app.lines), encoding="utf-8")

    out = Path(args.out)
    if out.parent != Path(""):
        out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(document, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    paths = document.get("paths") or {}
    print("{0} 에 저장했다. 포트 {1}, 최상위 키 {2}, paths {3}개".format(
        out, port, sorted(document.keys()), len(paths)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
