"""G0 — 작업 폴더에서 컴파일과 부팅, 층 사이의 의존 방향.

작업 폴더는 run이 시작될 때 스켈레톤을 복사해 만든 완성된 Maven 프로젝트다. 생성기가 그 폴더를 제자리에서
고치고 게이트가 같은 폴더에서 바로 판정한다. 그래서 이 게이트는 파일을 옮기지 않는다. 임시 폴더로 옮겨
돌리면 Maven의 `target/`이 매번 비어 iteration마다 전체 빌드가 되고, 제자리에서 고치기로 한 이점이 사라진다.

`mvnw test`가 컴파일과 ArchUnit을 돌리고, 스켈레톤이 함께 들고 있는 `tools/dump_api_docs.py`가 부팅해
springdoc이 내는 `/v3/api-docs`를 받는다. AI가 자기 구현을 설명한 문서를 받지 않는 것이 핵심이다.
받으면 "스펙대로 짰느냐"를 스펙대로 짠 쪽에 되묻는 일이 된다.

작업 폴더나 추출 스크립트가 없으면 `status: "ERROR"`다. 판정 장치가 없는 것은 통과가 아니다. 게이트 공백은
미판정이고 미판정은 통과시키지 않는다. 종료 상태를 REJECT로 두지 않는 이유는 초안의 잘못이 아니라 단계 실행
자체가 진행하지 못한 것이어서, 그 상태를 REJECT로 적으면 refine이 고칠 수 없는 것을 고치려 든다.

G0가 깨지면 runner가 critique와 eval을 건너뛰고 바로 refine으로 보낸다. 컴파일되지 않는 초안에는 비평할 것도
채점할 것도 없다. 건너뛴 이유는 원장에 남는다.

그래서 위반의 `detail`이 다음 초안이 받는 **유일한 신호**다. 요약이 원인을 담지 못하면 게이트가 옳게 잡아도
루프가 고칠 수 없다. 이 게이트가 실패를 요약할 때 프레임을 버리고 원인을 먼저 담는 이유다.

반환은 BUILD.md가 고정한 모양이다.

    {"status": "PASS"|"REJECT"|"ERROR", "violations": [...], "derived_spec": <path or None>, "log": str}
"""

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

# 추출 스크립트는 스켈레톤이 소유하고, 작업 폴더는 스켈레톤 사본이므로 그 안에 있다.
EXTRACTOR = ("tools", "dump_api_docs.py")

ARCH_MARKERS = ("LayerBoundaryTest", "ArchUnit", "com.tngtech.archunit", "ArchRule")

MIN_JAVA_MAJOR = 17

# 쓸 JDK를 고르는 것은 사람의 판단이다. 추출 스크립트가 다른 JDK를 찾아 나서지 않는 것과 같은 이유로
# 이 게이트도 찾아 나서지 않고, 사람이 정한 값을 이 환경 변수나 JAVA_HOME에서 받는다.
JAVA_HOME_ENV = "CONTRACT_IMPL_JAVA_HOME"

BUILD_TIMEOUT = 900
EXTRACT_TIMEOUT = 900

EXTRACTOR_EXIT = {
    2: "스켈레톤 경로나 JDK 버전이나 메이븐이 잘못됐다",
    3: "컴파일·패키징이 실패했다",
    4: "부팅이 실패했거나 제한 시간 안에 포트를 열지 못했다",
    5: "부팅은 했지만 api-docs를 받지 못했다",
}


def violation(rule, where, detail):
    return {"rule": rule, "point": None, "where": where, "detail": detail, "verdict": "violation"}


def error(rule, where, detail):
    return {"rule": rule, "point": None, "where": where, "detail": detail, "verdict": "error"}


def maven_command(project):
    if os.name == "nt":
        wrapper = project / "mvnw.cmd"
        if wrapper.exists():
            return [str(wrapper)]
    wrapper = project / "mvnw"
    if wrapper.exists():
        return ["sh", str(wrapper)] if os.name == "nt" else [str(wrapper)]
    found = shutil.which("mvn")
    return [found] if found else None


# 도구의 출력 인코딩. 윈도우의 도구들은 콘솔 코드페이지로 한국어를 낸다. 앞의 것부터 시도하고
# 둘 다 실패하면 대체 문자로 푼다.
OUTPUT_CODECS = ("utf-8", "cp949")


def decode(raw):
    """도구 출력의 바이트를 문자로 푼다. 어떤 바이트가 와도 예외를 내지 않는다.

    판정이 도구의 출력 인코딩 때문에 멈추면 안 된다. 그것은 초안의 잘못도 계약의 잘못도 아니고, 멈추면
    그 iteration이 무엇 때문에 깨졌는지 아무도 모른다.

    `errors="replace"` 하나로 끝내지 않는 이유는 바이트를 살리는 편이 낫기 때문이다. 코드페이지로 쓴
    한국어를 대체 문자로 바꿔 버리면 javac이 한국어로 낸 오류 문장이 요약에서 물음표가 된다.

    한 출력 안에 인코딩이 섞이기도 한다. Maven은 자기 줄을 UTF-8로 내고 javac은 같은 파이프에 코드페이지로
    낸다. 그래서 전체를 UTF-8로 푸는 데 실패하면 줄 단위로 내려가 줄마다 다시 고른다. 한 줄이 어떤
    인코딩으로도 풀리지 않아도 나머지 줄의 원인은 살아남는다.
    """
    if isinstance(raw, str):
        return raw.replace("\r\n", "\n")
    if not raw:
        return ""
    try:
        return raw.decode("utf-8").replace("\r\n", "\n")
    except UnicodeDecodeError:
        pass
    return "\n".join(decode_line(line) for line in raw.replace(b"\r\n", b"\n").split(b"\n"))


def decode_line(raw):
    for codec in OUTPUT_CODECS:
        try:
            return raw.decode(codec)
        except (UnicodeDecodeError, LookupError):
            continue
    return raw.decode("utf-8", errors="replace")


def run(command, cwd, timeout, java_home=None):
    """도구를 부르고 (종료 코드, 출력)을 돌려준다. 출력은 바이트로 받아 여기서 푼다.

    `text=True`로 받으면 디코딩이 파이프를 읽는 스레드 안에서 일어나 그 자리에서 예외가 오르고,
    종료 코드조차 받지 못한다. 실제로 그 일이 일어나 run 하나가 판정 없이 끝났다.
    """
    environment = dict(os.environ, PYTHONIOENCODING="utf-8")
    if java_home:
        environment["JAVA_HOME"] = str(java_home)
    try:
        done = subprocess.run(command, cwd=str(cwd), timeout=timeout, env=environment,
                              capture_output=True)
        return done.returncode, decode(done.stdout) + decode(done.stderr)
    except subprocess.TimeoutExpired as expired:
        partial = decode(getattr(expired, "stdout", None)) + decode(getattr(expired, "stderr", None))
        head = f"제한 시간 {timeout}초를 넘겼다: {' '.join(str(part) for part in command)}"
        return None, f"{head}\n{partial}" if partial else head
    except OSError as problem:
        return None, f"실행하지 못했다: {problem}"
    except UnicodeError as problem:
        # decode 가 예외를 내지 않게 썼지만, 내더라도 판정을 멈추지 않는다.
        return None, f"출력을 읽지 못했다: {problem}"


def java_major(java_home):
    """`<java_home>/bin/java -version`이 말하는 주 버전. 읽지 못하면 None."""
    binary = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")
    if not binary.exists():
        return None
    code, output = run([str(binary), "-version"], Path(java_home), 60)
    if code != 0:
        return None
    found = re.search(r'version "?(\d+)(?:\.(\d+))?', output)
    if not found:
        return None
    first = int(found.group(1))
    return int(found.group(2) or 0) if first == 1 else first


# ── 실패를 어떻게 요약하는가 ───────────────────────────────────────────────
#
# G0가 깨진 iteration은 비평과 채점을 건너뛰므로 위반의 `detail`이 다음 초안이 받는 유일한 신호다.
# 그래서 요약은 원인을 먼저 담는다. 스택 프레임은 가장 값이 없는 부분인데 가장 길어서, 앞에서부터 자르면
# 프레임만 남고 원인이 잘린다. 실제로 그 일이 일어나 루프가 세 iteration을 헤맸다.
#
# 전체 로그는 `log`에 그대로 남는다. 사람이 읽을 자리는 그쪽이고, 잘라 내는 것은 `detail`뿐이다.

DETAIL_LIMIT = 1600

# 값이 없는 줄. 프레임과 Maven·JVM의 맺음말이다.
FRAME = re.compile(r"^\s*(at\s+\S+\(|\.{3}\s+\d+\s+more\s*$|Suppressed:)")
BUILD_NOISE = (
    "Failed to execute goal", "-> [Help", "To see the full stack trace", "Re-run Maven",
    "For more information about the errors", "COMPILATION ERROR", "BUILD FAILURE",
    "[INFO]", "[WARNING] ", "***************", "APPLICATION FAILED TO START",
)

# 로그 한 줄 앞의 시각·프로세스·로거 머리. 원인과 무관하고 자리를 많이 먹는다.
LOG_PREFIX = re.compile(r"^\d{4}-\d{2}-\d{2}T[\d:.+]+\s+\w+\s+\d+\s+---\s+\[[^\]]*\]\s+\[[^\]]*\]\s+\S+\s*:\s*")
# 같은 예외를 두 번 적는 자리. 스프링은 초기화 중 경고로 한 번, 부팅 실패로 한 번 낸다.
# 이 머리를 걷어 내면 두 줄이 같아져 한 줄로 합쳐진다.
RESTATEMENT = (
    "Exception encountered during context initialization - cancelling refresh attempt: ",
)
# 빈이 어디서 왔는지를 적은 중첩 jar URL. 클래스 이름만 남긴다.
DEFINED_IN = re.compile(r"\s*defined in (?:URL|file|class path resource|BeanDefinition) \[[^\]]*?([\w$]+\.class)[^\]]*\]")
DEFINED_IN_ANY = re.compile(r"\s*defined in (?:URL|file|class path resource|BeanDefinition) \[[^\]]*\]")

CAUSED_BY = re.compile(r"^\s*Caused by:\s*(.+)$")
# 스프링이 프레임 위에 함께 내는, 사람이 읽는 진단.
BOOT_DIAGNOSTIC = re.compile(
    r"(Error creating bean with name|Parameter \d+ of constructor|No qualifying bean|"
    r"required a bean of type|Failed to instantiate|No default constructor|"
    r"Unsatisfied dependency|Consider defining a bean|BeanDefinitionOverrideException|"
    r"Circular (?:reference|depends)|Port \d+ was already in use)")
# Spring Boot 의 실패 분석기가 내는 절. 있으면 그것이 가장 좋은 요약이다.
BOOT_REPORT = ("Description:", "Action:")

COMPILE_ERROR = re.compile(r"^\[ERROR\]\s+(\S+\.(?:java|kt)):\[(\d+),(\d+)\]\s+(.*)$")
# javac 의 오류 문장은 JDK 의 로케일을 따라 번역된다(`symbol:`이 `기호:`로 온다). 이름표를 영어로만
# 찾으면 다른 로케일에서 아무것도 못 찾으므로, 오류 줄 아래의 들여쓴 줄을 이름표를 묻지 않고 받는다.
COMPILE_DETAIL = re.compile(r"^(?:\[ERROR\])?\s{2,}(\S.*)$")
# 층 경계를 어긴 자리. ArchUnit 이 규칙 이름과 어긴 자리를 함께 낸다.
ARCH_LINE = re.compile(r"(Architecture Violation|was violated|^\s*Rule\s|Tests run:.*(?:Failures|Errors): [1-9])")

EXTRACTOR_REASON = re.compile(r"^dump_api_docs:\s*(.+)$")


def scrub(line):
    """한 줄에서 값이 없는 부분을 걷어 낸다. 원인을 담을 자리를 남기려는 것이다."""
    line = LOG_PREFIX.sub("", line.rstrip())
    for head in RESTATEMENT:
        if line.startswith(head):
            line = line[len(head):]
    line = DEFINED_IN.sub(r" defined in \1", line)
    line = DEFINED_IN_ANY.sub("", line)
    return line.strip()


def join_labels(lines):
    """`Description:`과 `Action:` 뒤에 오는 본문을 그 줄에 붙인다.

    스프링은 이름표와 본문 사이에 빈 줄을 둔다. 빈 줄을 버리고 줄 단위로 고르면 이름표만 남고 본문이
    다른 자리로 흩어져, 요약을 읽는 쪽이 둘을 다시 이어야 한다.
    """
    joined, skip = [], set()
    for index, line in enumerate(lines):
        if index in skip:
            continue
        if line in BOOT_REPORT and index + 1 < len(lines):
            joined.append(f"{line} {lines[index + 1]}")
            skip.add(index + 1)
        else:
            joined.append(line)
    return joined


def meaningful(text):
    """프레임과 맺음말을 버린 줄."""
    for line in (text or "").splitlines():
        if not line.strip() or FRAME.match(line):
            continue
        if any(noise in line for noise in BUILD_NOISE):
            continue
        yield line


def assemble(parts, limit=DETAIL_LIMIT):
    """원인을 먼저 담고 남는 자리에 맥락을 담는다.

    첫 항목은 한도를 넘어도 넣는다. 원인 한 줄이 잘려 나가면 요약이 아무 일도 하지 않은 것이 된다.
    """
    kept, used, dropped = [], 0, 0
    for part in dict.fromkeys(p for p in parts if p):
        if kept and used + len(part) + 1 > limit:
            dropped += 1
            continue
        kept.append(part)
        used += len(part) + 1
    if kept and len(kept[0]) > limit:
        kept[0] = kept[0][:limit] + " …"
    if dropped:
        kept.append(f"(맥락 {dropped}줄 줄임. 전문은 로그에 있다)")
    return "\n".join(kept)


def summarize_boot_failure(text, limit=DETAIL_LIMIT):
    """부팅 실패의 원인을 먼저 담는다.

    `Caused by:` 사슬의 마지막 원인이 실제 원인이므로 그것을 첫 줄에 둔다. 스프링이 프레임 위에 함께 내는
    진단(`Error creating bean with name '...'`, `Parameter N of constructor ...`)이 그 원인이 어느 빈에서
    났는지 말해 주므로 그다음에 둔다. `Description:`과 `Action:` 절이 있으면 그것이 가장 좋은 요약이다.
    """
    lines = [scrub(line) for line in meaningful(text)]
    lines = [line for line in lines if line]
    lines = join_labels(lines)

    causes = [found.group(1).strip() for line in lines for found in [CAUSED_BY.match(line)] if found]
    report = [line for line in lines
              if any(line.startswith(mark) for mark in BOOT_REPORT) and line not in BOOT_REPORT]
    diagnostics = [line for line in lines if BOOT_DIAGNOSTIC.search(line) and not CAUSED_BY.match(line)]
    reason = [found.group(1).strip() for line in lines for found in [EXTRACTOR_REASON.match(line)] if found]

    parts = []
    if causes:
        parts.append("원인: " + causes[-1])
        for cause in reversed(causes[:-1]):
            parts.append("  ← " + cause)
    parts.extend(report)
    parts.extend(diagnostics[:4])
    if reason:
        parts.append("추출기: " + reason[0])
    if not parts:
        parts = lines[-6:]
    return assemble(parts, limit)


def summarize_build_failure(text, limit=DETAIL_LIMIT):
    """컴파일 실패는 javac 의 오류 줄만, 층 경계 위반은 규칙과 어긴 자리만 남긴다."""
    lines = [line.rstrip() for line in meaningful(text)]

    errors, details, count = [], [], 0
    for line in lines:
        found = COMPILE_ERROR.match(line)
        if found:
            count += 1
            where = found.group(1)
            if "/src/" in where:
                where = "src/" + where.split("/src/", 1)[1]
            errors.append(f"{where}:{found.group(2)}:{found.group(3)} {found.group(4).strip()}")
            continue
        detail = COMPILE_DETAIL.match(line)
        if detail and errors:
            details.append("    " + re.sub(r"\s+", " ", scrub(detail.group(1))))

    if errors:
        unique = list(dict.fromkeys(errors))
        parts = [f"컴파일 오류 {len(unique)}건"] + unique[:6] + list(dict.fromkeys(details))[:6]
        if len(unique) > 6:
            parts.insert(1, f"(앞 6건만 적는다)")
        return assemble(parts, limit)

    arch = [scrub(line) for line in lines if ARCH_LINE.search(line)]
    if arch:
        return assemble(["층 경계나 테스트가 깨졌다"] + [line for line in arch if line][:10], limit)

    return assemble([scrub(line) for line in lines][-8:], limit)


def tail(text, lines=40):
    kept = [line for line in (text or "").splitlines() if line.strip()][-lines:]
    return "\n".join(kept)


def run_g0(project_dir, work_dir, java_home=None):
    """작업 폴더에서 제자리에 컴파일하고 ArchUnit을 돌리고 추출 스펙을 받는다.

    `project_dir`는 run의 작업 폴더다. 스켈레톤 사본과 생성 코드가 한 자리에 있는 완성된 Maven 프로젝트여서
    draft를 어디에 떨어뜨릴 필요가 없다. `work_dir`에는 그 iteration의 게이트 산출만 쓴다. 추출 스펙을
    프로젝트 안에 쓰면 `mvn clean`이 지우거나 다음 iteration이 덮으므로, 원장이 iteration마다 되짚을 수 있게
    밖에 둔다.

    `java_home`은 BUILD.md의 시그니처에 없던 인자다. 스켈레톤은 JDK 17 이상을 요구하는데 기계의
    `JAVA_HOME`이 그보다 낮으면 컴파일이 실패하고, 그것은 초안의 잘못이 아니라 환경 문제라 REJECT가 아니라
    ERROR여야 한다. 쓸 JDK를 고르는 것은 사람의 판단이므로 찾아 나서지 않고 이 인자나 환경 변수
    `CONTRACT_IMPL_JAVA_HOME`으로 받는다. 넘기지 않으면 `JAVA_HOME`을 쓴다.
    """
    log = []
    project = Path(project_dir)
    work_dir = Path(work_dir)
    java_home = java_home or os.environ.get(JAVA_HOME_ENV) or os.environ.get("JAVA_HOME")

    if not project.exists() or not (project / "pom.xml").exists():
        return {"status": "ERROR", "derived_spec": None,
                "violations": [error("g0.project_missing", str(project),
                                     "pom.xml이 있는 작업 폴더를 찾지 못했다. 구현의 계약을 뽑을 자리가 없다")],
                "log": f"작업 폴더가 없다: {project}"}

    extractor = project.joinpath(*EXTRACTOR)
    if not extractor.exists():
        return {"status": "ERROR", "derived_spec": None,
                "violations": [error("g0.extractor_missing", "/".join(EXTRACTOR),
                                     "추출 스크립트를 찾지 못했다. AI가 쓴 설명으로 대신하지 않는다")],
                "log": f"추출 스크립트가 없다: {extractor}"}

    major = java_major(java_home) if java_home else None
    if java_home and major is not None and major < MIN_JAVA_MAJOR:
        return {"status": "ERROR", "derived_spec": None,
                "violations": [error("g0.jdk_unusable", str(java_home),
                                     f"스켈레톤은 JDK {MIN_JAVA_MAJOR} 이상을 요구하는데 {major}를 가리킨다. "
                                     f"쓸 JDK는 {JAVA_HOME_ENV}로 넘긴다")],
                "log": f"JDK가 낮다: {java_home} (major {major})"}

    command = maven_command(project)
    if command is None:
        return {"status": "ERROR", "derived_spec": None,
                "violations": [error("g0.project_missing", "mvnw", "메이븐 래퍼도 mvn도 없다")],
                "log": f"메이븐이 없다: {project}"}

    work_dir.mkdir(parents=True, exist_ok=True)
    incremental = (project / "target" / "classes").exists()
    log.append(f"작업 폴더: {project} (target/ 재사용 {'예' if incremental else '아니오'})")

    code, output = run(command + ["-B", "-q", "test"], project, BUILD_TIMEOUT, java_home)
    log.append(f"$ {' '.join(command)} -B -q test → {code}")
    log.append(output)
    if code != 0:
        arch = any(marker in output for marker in ARCH_MARKERS)
        rule = "g0.arch_boundary" if arch else "g0.compile_failed"
        return {"status": "REJECT", "derived_spec": None,
                "violations": [violation(rule, "mvnw test", summarize_build_failure(output))],
                "log": "\n".join(log)}

    derived = work_dir / "api-docs.json"
    extract_command = [sys.executable, str(extractor), "--skeleton", str(project), "--out", str(derived)]
    if java_home:
        extract_command += ["--java-home", str(java_home)]
    code, output = run(extract_command, project, EXTRACT_TIMEOUT, java_home)
    log.append(f"$ dump_api_docs.py → {code}")
    log.append(output)
    if code != 0 or not derived.exists():
        reason = EXTRACTOR_EXIT.get(code, f"종료 코드 {code}")
        # 컴파일·패키징 단계에서 막혔으면 javac 의 오류 줄이 원인이고, 부팅에서 막혔으면 예외 사슬이 원인이다.
        summarize = summarize_build_failure if code == 3 else summarize_boot_failure
        return {"status": "REJECT", "derived_spec": None,
                "violations": [violation("g0.extract_failed", "dump_api_docs.py",
                                         f"{reason}\n{summarize(output)}")],
                "log": "\n".join(log)}

    return {"status": "PASS", "derived_spec": str(derived), "violations": [], "log": "\n".join(log)}
