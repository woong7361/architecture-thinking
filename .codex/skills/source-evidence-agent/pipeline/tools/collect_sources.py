"""1차 출처 스냅샷을 수집하고 해시 원장을 남긴다.

스냅샷 자체는 저장소에 넣지 않는다. 명세와 이 스크립트와 원장이 있으면 같은 스냅샷을 다시 만들 수 있고,
원장의 해시로 그것이 같은 스냅샷인지 확인할 수 있다.

사용법:
    python collect_sources.py                 # rules/sources.yaml 의 모든 루트를 수집
    python collect_sources.py --verify        # 수집하지 않고 원장과 현재 스냅샷을 대조만 한다
    python collect_sources.py --root <이름>   # 루트 하나만
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

import yaml

# Windows 콘솔의 기본 코드 페이지가 cp949 라 한글과 em dash 를 출력하다 죽는다.
for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

SKILL_ROOT = Path(__file__).resolve().parents[2]
MANIFEST = SKILL_ROOT / "rules" / "sources.yaml"
SOURCES_DIR = SKILL_ROOT / "sources"
LEDGER = SKILL_ROOT / "sources" / "LEDGER.json"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def rollup(root: Path) -> tuple[str, int]:
    """루트 아래 모든 파일의 경로와 내용을 정렬해 하나의 digest 로 만든다.

    파일 하나하나의 해시를 원장에 적지 않는 이유는 수천 개가 되기 때문이다. 인용된 파일의 해시는
    인용 시점에 게이트가 따로 기록한다.
    """
    digest = hashlib.sha256()
    count = 0
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix()
        digest.update(rel.encode("utf-8"))
        digest.update(sha256_file(path).encode("ascii"))
        count += 1
    return digest.hexdigest(), count


def expand(value: str) -> str:
    return os.path.expandvars(value)


def wanted(name: str, includes: list[str]) -> bool:
    return any(name.startswith(prefix) for prefix in includes)


def extract_zip(archive: Path, includes: list[str], dest: Path) -> None:
    with zipfile.ZipFile(archive) as zf:
        for info in zf.infolist():
            if info.is_dir() or not wanted(info.filename, includes):
                continue
            target = dest / info.filename
            target.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as src, target.open("wb") as out:
                shutil.copyfileobj(src, out)


def maven() -> str:
    """Windows 에서는 PATH 에 mvn.cmd 만 있어 이름 그대로는 실행되지 않는다."""
    for name in ("mvn", "mvn.cmd"):
        found = shutil.which(name)
        if found:
            return found
    raise FileNotFoundError("mvn 을 PATH 에서 찾지 못했다")


def fetch_sources_jar(coords: str, work: Path) -> Path:
    """mvn dependency:copy 로 sources 분류자 jar 를 받는다."""
    group, artifact, version = coords.split(":")
    subprocess.run(
        [
            maven(), "-B", "-ntp", "dependency:copy",
            f"-Dartifact={group}:{artifact}:{version}:jar:sources",
            f"-DoutputDirectory={work}",
        ],
        check=True,
        stdout=subprocess.DEVNULL,
    )
    jar = work / f"{artifact}-{version}-sources.jar"
    if not jar.exists():
        raise FileNotFoundError(f"sources jar 를 받지 못했다: {jar}")
    return jar


def collect_root(spec: dict) -> dict:
    name = spec["root"]
    dest = SOURCES_DIR / name
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True)

    archives: list[dict] = []
    includes = spec["include"]

    if spec["kind"] == "zip":
        archive = Path(expand(spec["archive"]))
        if not archive.exists():
            raise FileNotFoundError(f"아카이브가 없다: {archive}")
        extract_zip(archive, includes, dest)
        archives.append({"from": str(archive), "sha256": sha256_file(archive)})
    elif spec["kind"] == "maven":
        with tempfile.TemporaryDirectory() as tmp:
            work = Path(tmp)
            for coords in spec["coords"]:
                jar = fetch_sources_jar(coords, work)
                extract_zip(jar, includes, dest)
                archives.append({"from": coords, "sha256": sha256_file(jar)})
    else:
        raise ValueError(f"모르는 kind: {spec['kind']}")

    digest, count = rollup(dest)
    if count == 0:
        raise RuntimeError(f"{name}: include 가 아무 파일도 고르지 못했다")
    return {
        "root": name,
        "artifact": spec["artifact"],
        "grade": spec["grade"],
        "include": includes,
        "archives": archives,
        "files": count,
        "rollup_sha256": digest,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root")
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()

    manifest = yaml.safe_load(MANIFEST.read_text(encoding="utf-8"))
    specs = [s for s in manifest["roots"] if not args.root or s["root"] == args.root]
    if not specs:
        print(f"그런 루트가 없다: {args.root}", file=sys.stderr)
        return 2

    if args.verify:
        if not LEDGER.exists():
            print("원장이 없다. 먼저 수집한다.", file=sys.stderr)
            return 2
        recorded = {e["root"]: e for e in json.loads(LEDGER.read_text(encoding="utf-8"))["roots"]}
        failed = False
        for spec in specs:
            name = spec["root"]
            entry = recorded.get(name)
            if entry is None:
                print(f"MISSING  {name} — 원장에 없다")
                failed = True
                continue
            root = SOURCES_DIR / name
            if not root.exists():
                print(f"MISSING  {name} — 스냅샷이 없다")
                failed = True
                continue
            digest, count = rollup(root)
            if digest == entry["rollup_sha256"] and count == entry["files"]:
                print(f"OK       {name} — {count}개 파일")
            else:
                print(f"MISMATCH {name} — 원장 {entry['files']}개/{entry['rollup_sha256'][:12]}, "
                      f"현재 {count}개/{digest[:12]}")
                failed = True
        return 1 if failed else 0

    SOURCES_DIR.mkdir(parents=True, exist_ok=True)
    entries = []
    if LEDGER.exists() and args.root:
        entries = [e for e in json.loads(LEDGER.read_text(encoding="utf-8"))["roots"]
                   if e["root"] != args.root]
    for spec in specs:
        entry = collect_root(spec)
        entries.append(entry)
        print(f"수집 {entry['root']} — {entry['files']}개 파일, {entry['rollup_sha256'][:12]}")

    entries.sort(key=lambda e: e["root"])
    LEDGER.write_text(
        json.dumps({"resolved_from": manifest["resolved_from"], "roots": entries},
                   ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"원장: {LEDGER}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
