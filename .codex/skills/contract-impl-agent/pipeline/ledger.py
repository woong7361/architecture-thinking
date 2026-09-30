"""원장. `<run_dir>/run_ledger.json`을 iteration마다 갱신한다.

절은 여섯이다. 위반, 결정, 계약 변경, 기록, 영향, 그리고 주장과 판정의 불일치다. 앞의 셋은 무엇이 일어났는지를
id로 이어 붙이고, 기록과 영향은 판정이 아닌 셈을 담고, 마지막 절은 모델이 한 말과 장치가 낸 판정이 갈린 자리를
모은다. 통과 여부만 남기면 판단을 기록한 뜻이 없으므로 원장은 통과한 run에서도 같은 여섯 절을 낸다.

## 여섯 규칙

1. **위반 id는 안정적이다.** 규칙 id와 판정 지점과 좌표를 해시하므로 iteration이 바뀌어도 같은 위반이 같은
   id를 갖는다. 결정과 계약 변경은 모델이 붙인 id를 그대로 이어 쓴다.
2. **코드로 닫은 것과 계약으로 닫은 것을 가른다.** 열려 있던 위반이 사라졌을 때 그 위반의 좌표가 새 계약
   판본에서 없어졌거나 요구가 약해졌으면 `closed_by`가 `contract`고, 아니면 `code`다. 요구를 지워 위반을 없앤
   것이 "고쳤다"로 집계되면 원장이 거짓말을 한다.
3. **주장과 판정을 분리한다.** `claimed_fix`는 refine이 낸 문장이고 `verified_fix`는 작업 폴더가 앞 iteration과
   달라진 자리를 대조해 만든 것이다. 닫힘은 게이트 결과만 줄 수 있다.
4. **재발을 센다.** 닫혔던 id가 다시 열리면 `regressed`, 두 번 재발하면 `stuck`이다.
5. **곁가지 변경을 기록한다.** refine이 열린 위반과 무관한 파일을 바꿨으면 막지 않고 `collateral_change`로 남긴다.
6. **말없는 되돌림을 잡는다.** 서 있는 결정이 `supersedes` 없이 사라졌거나 고른 것이 달라졌으면
   `silent_reversal`이다.

## 판정을 결정적으로 하는 방법

좌표 판정은 계약 판본 두 벌을 비교해 한다. 비교기는 `gates/schema_compare.py`이고 그 원본은
`phase2/taskE/task5/gate/breaking_gate.py`다. 판본 사이에서는 요청이 좁아지는 것과 응답이 사라지는 것이 모두
깨는 변경이므로 그 게이트의 판정을 그대로 쓴다. 판정 지점의 좌표 규약도 게이트와 같은
`operationId:status`와 `operationId:CODE`다.

위반 id의 해시에 게이트가 쓴 `detail` 문장은 넣지 않는다. `detail`에는 개수와 목록이 들어가서 같은 위반인데도
iteration마다 문장이 달라지고, 그러면 id가 흔들려 재발을 셀 수 없다. 대신 `rule`과 `point`와 `where`를 넣는다.
이 셋이 같은 두 위반은 같은 자리의 같은 손해다.

## 대조 대상은 작업 폴더 하나다

생성기는 파일 내용을 응답에 담지 않고 run 폴더의 작업 폴더 `output/`에 실물을 쓴다. 매니페스트는 그 경로의
색인일 뿐이다. 작업 폴더는 iteration마다 새로 만들지 않고 제자리에서 고치므로 앞 상태가 디스크에 남지 않는다.
그래서 초안 사이의 변경을 보는 규칙들은 상태를 두 군데서 얻는다.

- **지금 상태**는 `output/`을 훑어 만든다. 파일 목록은 git이 쓰는 목록과 같게 뽑는다. 같게 뽑지 않으면 원장이
  세는 변경과 runner가 `git diff`로 판정하는 변경이 갈라진다.
- **앞 상태의 변경 탐지**는 원장이 직전 iteration에 적어 둔 파일별 sha256 색인으로 한다. git이 없어도 돌고,
  추가와 삭제와 수정이 그 비교에서 결정적으로 나온다.
- **앞 상태의 본문**은 git에서 꺼낸다. `git show <앞 커밋>:<경로>`다. 커밋 해시는 draft에 적혀 있다. 없으면
  그 iteration은 발췌 diff 없이 변경만 센다. 셈이 본문에 기대지 않게 나눠 둔 이유가 이것이다.

`silent_reversal`은 이 대조에 기대지 않는다. 결정은 파일이 아니라 draft JSON의 판단 기록에 있다.

계약 판본은 `output/contract/<이름>-v<n>.yaml`이고 앞 판본을 지우지 않으므로 같은 폴더에 함께 남는다.
`closed_by`를 가르는 판본 대조는 `contract_version.from`이 가리키는 그 파일을 그 자리에서 열고, 없으면 git에서
꺼낸다. 코드 변경에서는 계약 판본 경로를 뺀다. 빼지 않으면 "확인된 수정"이 계약 전문 diff로 덮여 코드가 실제로
달라졌는지가 가려진다. 판본이 움직인 것은 리포트 2절과 3절이 좌표별로 편다.

## runner와의 접점

`update_ledger(run_dir, iteration, payload)`를 부른다. payload에는 draft 자체가 아니라 run 폴더 기준의 draft
경로가 실려 오므로 원장이 그 파일을 열어 색인과 판단 기록과 커밋 해시를 읽는다. 앞 iteration의 해시 색인과
커밋 해시는 원장이 직전에 자기 손으로 적어 둔 항목에서 찾는다.
"""

from __future__ import annotations

import difflib
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

PIPELINE_DIR = Path(__file__).resolve().parent
if str(PIPELINE_DIR) not in sys.path:
    sys.path.insert(0, str(PIPELINE_DIR))

KST = timezone(timedelta(hours=9))
LEDGER_NAME = "run_ledger.json"
OUTPUT_DIR = "output"

# runner가 커밋 해시를 draft에 적는 키. 어느 이름으로 와도 받는다.
COMMIT_KEYS = ("commit", "commit_sha", "output_commit", "git_commit", "output_commit_sha")

STATUS_OPEN = "open"
STATUS_CLOSED = "closed"
STATUS_REGRESSED = "regressed"
STATUS_STUCK = "stuck"

# Critique가 심각하다고 한 축에 Eval이 이 점수 이상을 줬으면 둘이 갈린 것으로 본다.
# 루브릭의 `min_axis`가 4.0이므로 그 이상은 "통과시킬 만하다"는 판정이고, high 지적과 함께 설 수 없다.
AXIS_HIGH_SCORE = 4.0

DIFF_EXCERPT_LINES = 40
TOKEN = re.compile(r"[A-Za-z_][A-Za-z0-9_]{2,}")

# 대조기가 "대조하지 못했다"고 적은 줄을 가르는 표지. 그 줄은 깨는 변경이 아니라 미판정이다.
#
# 대조기는 판정을 문장으로 낸다. 그래서 종류를 가르는 유일한 방법이 그 문장을 보는 것이고, 표지가 바뀌면
# 여기도 따라 고쳐야 한다. 그 결합을 감수하는 이유는 섞어 세는 쪽이 더 나쁘기 때문이다. 미판정을 깨는 변경으로
# 세면 리포트를 읽는 사람이 계약이 깨졌다고 믿고, 미판정을 조용히 버리면 대조되지 않은 자리가 통과로 읽힌다.
# 게이트 공백은 통과가 아니라는 원칙이 여기서도 같다.
UNRESOLVED_REF = "x-unresolved-ref"
UNCOMPARED_MARKS = ("대조하지 못했다", UNRESOLVED_REF)

# 이보다 큰 파일은 해시로만 비교한다. 자바 소스와 계약 판본은 이 아래이고, 빌드 산출물이 `files/`에 섞여 들어와도
# 원장이 그 전문을 읽어 메모리와 리포트를 채우지 않게 한다.
MAX_DIFF_BYTES = 256 * 1024


# ── 파일 읽기 ───────────────────────────────────────────────────────────────

def _now() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def _read_json(path: Path) -> dict:
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return data if isinstance(data, dict) else {}


def _read_contract(path: Path) -> dict:
    """계약 판본 파일을 읽는다. YAML과 JSON을 둘 다 받는다."""
    if not path.exists():
        return {}
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() in (".yaml", ".yml"):
        import yaml

        data = yaml.safe_load(text)
    else:
        data = json.loads(text)
    return data if isinstance(data, dict) else {}


def _safe_rel(raw: str) -> str:
    rel = str(raw or "").strip().replace("\\", "/").lstrip("/")
    parts = [p for p in rel.split("/") if p not in ("", ".")]
    if not parts or any(p == ".." for p in parts):
        raise ValueError(f"unsafe path in ledger input: {raw!r}")
    return "/".join(parts)


def output_dir_of(run_dir: Path, declared=None) -> Path:
    """작업 폴더. iteration마다 새로 만들지 않고 제자리에서 고친다. 생성기의 작업 디렉터리가 여기다.

    runner가 payload에 그 경로를 실어 주면 그것을 쓴다. 폴더 이름을 여기에 두 번 적어 두면 한쪽이 바뀔 때
    원장이 빈 폴더를 훑고 변경 0건을 내면서 조용히 틀린다.
    """
    if declared:
        return Path(run_dir) / _safe_rel(str(declared))
    return Path(run_dir) / OUTPUT_DIR


def commit_of(draft: dict) -> str:
    """그 iteration의 커밋 해시. runner가 draft에 적는다.

    키 이름을 하나로 못 박지 않는다. 원장이 쓰는 것은 발췌 diff의 옛 본문뿐이고, 해시가 없으면 발췌만 빠지고
    변경의 셈은 해시 색인으로 그대로 돌아가므로 이름이 어긋나 원장이 깨지는 것보다 넓게 받는 편이 낫다.
    """
    for source in (draft, draft.get("metadata") or {}):
        if not isinstance(source, dict):
            continue
        for key in COMMIT_KEYS:
            value = str(source.get(key) or "").strip()
            if value:
                return value
    return ""


def _git(output_dir: Path, *args: str) -> bytes | None:
    """`output/` 저장소에 git을 한 번 돌린다. git이 없거나 실패하면 None이다."""
    try:
        done = subprocess.run(
            ["git", "-C", str(output_dir), *args],
            capture_output=True, check=False,
        )
    except (FileNotFoundError, OSError):
        return None
    return done.stdout if done.returncode == 0 else None


def git_tracked_paths(output_dir: Path) -> list[str] | None:
    """git이 보는 파일 목록. `.gitignore`가 거른 것은 여기 없다.

    원장이 이 목록을 쓰는 이유는 runner가 `git diff`로 판정하기 때문이다. 목록이 갈리면 원장이 세는 변경과
    게이트가 판정하는 변경이 서로 다른 모집단 위에 선다. `target/`처럼 무시된 빌드 산출물도 같은 규칙으로 빠진다.
    """
    out = _git(output_dir, "ls-files", "--cached", "--others", "--exclude-standard")
    if out is None:
        return None
    return [line for line in out.decode("utf-8", "replace").splitlines() if line.strip()]


def git_skeleton_paths(output_dir: Path) -> set[str] | None:
    """첫 커밋에 든 파일들. `output/`은 스켈레톤을 복사해 만들고 첫 커밋을 남기므로 그것이 사람 소유다.

    생성기가 놓은 것과 처음부터 있던 것을 가르는 데 쓴다. 색인은 생성기가 자기가 쓴 것을 신고한 것이므로
    스켈레톤 사본이 색인에 없는 것은 어긋남이 아니다.
    """
    root = _git(output_dir, "rev-list", "--max-parents=0", "HEAD")
    if root is None:
        return None
    first = root.decode("utf-8", "replace").split()
    if not first:
        return None
    listing = _git(output_dir, "ls-tree", "-r", "--name-only", first[-1])
    if listing is None:
        return None
    return {line for line in listing.decode("utf-8", "replace").splitlines() if line.strip()}


def git_show(output_dir: Path, commit: str, rel_path: str) -> str | None:
    """앞 커밋의 그 파일 본문. 발췌 diff의 왼쪽이 여기서 온다.

    `git show <커밋>:<경로>`를 쓰지 않는다. git이 그 인자를 저장소 안의 경로로 확정하기 전에 파일시스템을
    확인하는데, run 폴더가 깊으면 윈도우의 경로 길이 한도에 걸려 `Filename too long`으로 실패한다. 대신
    트리에서 blob 해시를 찾아 그것을 읽는다. 이 경로는 파일시스템을 보지 않는다.
    """
    if not commit or not rel_path:
        return None
    listing = _git(output_dir, "ls-tree", "-z", commit, "--", rel_path)
    if not listing:
        return None
    sha = ""
    for record in listing.decode("utf-8", "replace").split("\0"):
        head = record.split("\t", 1)[0].split()
        if len(head) >= 3 and head[1] == "blob":
            sha = head[2]
            break
    if not sha:
        return None
    out = _git(output_dir, "cat-file", "blob", sha)
    if out is None or len(out) > MAX_DIFF_BYTES:
        return None
    try:
        return normalize_newlines(out.decode("utf-8"))
    except UnicodeDecodeError:
        return None


def contract_paths_of(draft: dict) -> set[str]:
    """이 draft가 계약 판본으로 가리키는 경로들. 계보의 정본은 `contract_version` 포인터다.

    색인의 `role`도 함께 본다. runner가 판본을 색인에 다른 이름으로 적는 날에도 코드 변경에서 빠지게 한다.
    """
    version = draft.get("contract_version") or {}
    paths = {str(version.get("path") or ""), str(version.get("from") or "")}
    for entry in (draft.get("files") or []):
        if isinstance(entry, dict) and str(entry.get("role") or "").strip() == "contract":
            paths.add(str(entry.get("path") or ""))
    return {p for p in paths if p}


def contract_doc(run_dir: Path, rel_path: str, prev_commit: str = "") -> tuple[dict, str]:
    """계약 판본 하나를 읽고 어디서 읽었는지 함께 돌려준다.

    판본이 쌓이고 앞 판본을 지우지 않으므로 이번 판본과 앞 판본이 모두 `output/` 아래에 있다. 거기 없으면
    앞 커밋에서 꺼내고, 그것도 없으면 기준선 사본을 본다. 세 자리를 다 보는 이유는 이 판정이 `closed_by`를
    가르기 때문이다. 판본을 읽지 못하면 계약으로 닫은 것이 코드로 닫은 것으로 집계된다.
    """
    if not rel_path:
        return {}, ""
    rel = _safe_rel(rel_path)
    output_dir = output_dir_of(run_dir)

    on_disk = output_dir / rel
    if on_disk.exists():
        return _parse_contract(on_disk.read_text(encoding="utf-8"), rel), f"{OUTPUT_DIR}/{rel}"

    from_git = git_show(output_dir, prev_commit, rel)
    if from_git is not None:
        return _parse_contract(from_git, rel), f"git:{prev_commit[:8]}:{rel}"

    baseline = run_dir / "baseline" / Path(rel).name
    if baseline.exists():
        return _parse_contract(baseline.read_text(encoding="utf-8"), rel), f"baseline/{baseline.name}"
    return {}, ""


def _parse_contract(text: str, rel: str) -> dict:
    if rel.lower().endswith((".yaml", ".yml")):
        import yaml

        data = yaml.safe_load(text)
    else:
        data = json.loads(text)
    return data if isinstance(data, dict) else {}


def _first_in(directory: Path) -> Path | None:
    """`baseline/`과 `origin/`에는 계약이 한 벌씩 들어간다. v0의 HTTP 모드는 한 벌만 쓴다."""
    if not directory.exists():
        return None
    files = sorted(p for p in directory.iterdir() if p.is_file())
    return files[0] if files else None


# ── 계약 좌표 ───────────────────────────────────────────────────────────────

def _gates():
    """게이트 모듈은 다른 조각이 소유한다. 없으면 좌표 판정을 포기하지 않고 줄인다.

    두 가지로 부른다. `pipeline/`이 경로에 있으면 `gates`가 최상위 패키지이고, 스킬 폴더가 경로에 있으면
    `pipeline.gates`다. 게이트 안에서 `from ..tools import ...`처럼 부모를 거슬러 올라가는 import가 있으면
    앞의 방식이 `attempted relative import beyond top-level package`로 깨지므로 뒤의 방식으로 한 번 더 시도한다.
    원장은 기록 장치이므로 그 자리에서 셈을 포기하지 않는다.
    """
    for loader in (_gates_flat, _gates_packaged):
        found = loader()
        if found is not None:
            return found
    return None


def _gates_flat():
    try:
        from gates.changes import diff_pointers, points_of
        from gates.schema_compare import compare_http
    except ImportError:
        return None
    return {"diff_pointers": diff_pointers, "points_of": points_of, "compare_http": compare_http,
            "loaded_as": "gates"}


def _gates_packaged():
    import importlib

    root = str(PIPELINE_DIR.parent)
    if root not in sys.path:
        sys.path.insert(0, root)
    try:
        changes = importlib.import_module("pipeline.gates.changes")
        compare = importlib.import_module("pipeline.gates.schema_compare")
    except ImportError:
        return None
    return {"diff_pointers": changes.diff_pointers, "points_of": changes.points_of,
            "compare_http": compare.compare_http, "loaded_as": "pipeline.gates"}


def _walk(node, on_dict):
    if isinstance(node, dict):
        on_dict(node)
        for value in node.values():
            _walk(value, on_dict)
    elif isinstance(node, list):
        for item in node:
            _walk(item, on_dict)


def demand_index(doc: dict, gates: dict | None) -> dict:
    """계약이 요구하는 것의 색인. 판정 지점과 필드 이름과 필수로 둔 이름을 담는다.

    좌표가 사라졌는지 요구가 약해졌는지를 판정하는 데 쓴다. 이름만으로 세는 이유는 위반의 `where`가
    `Alert.lastCheckedAt`처럼 스키마 이름과 필드 이름으로 오기 때문이다.
    """
    required: set[str] = set()
    properties: set[str] = set()

    def visit(node: dict) -> None:
        # 대조기가 풀지 못한 참조에 남긴 표시는 계약의 값이 아니다. 그 자리를 이름으로 세면 필드가 사라진
        # 것처럼 보여 `closed_by`가 뒤집힌다. 이 색인은 원문을 읽으므로 표시가 들어올 일이 없지만,
        # 펼친 문서가 들어와도 같은 판정이 나오게 막아 둔다.
        if UNRESOLVED_REF in node:
            return
        names = node.get("required")
        if isinstance(names, list):
            required.update(str(n) for n in names if isinstance(n, str) and n != UNRESOLVED_REF)
        if node.get("in") and node.get("name") and node.get("required") is True:
            required.add(str(node["name"]))
        props = node.get("properties")
        if isinstance(props, dict):
            properties.update(str(k) for k in props if k != UNRESOLVED_REF)

    _walk(doc or {}, visit)
    points = set(gates["points_of"](doc or {})) if gates else set()
    return {"points": points, "required": required, "properties": properties}


def where_leaf(where: str | None) -> str:
    """위반의 좌표에서 마지막 이름만 뽑는다. `Alert.items[].lastCheckedAt` → `lastCheckedAt`."""
    value = str(where or "").strip()
    if not value:
        return ""
    leaf = value.replace("[]", "").rstrip("/").split("/")[-1].split(".")[-1]
    return leaf.strip()


def violation_leaves(violation: dict) -> list[str]:
    """이 위반이 가리키는 이름들. 좌표의 잎과 접힌 잎에서 뽑는다.

    게이트가 스키마 하나의 차이를 한 건으로 접으면 좌표는 `Alert`처럼 부모가 되고 정작 어느 필드가 문제인지는
    `evidence`에 들어간다. 잎을 좌표에서만 뽑으면 그 위반이 계약으로 닫혔는지 알 수 없다. 접힌 잎도 함께 본다.
    """
    leaves = [where_leaf(violation.get("where"))]
    for item in (violation.get("evidence") or []):
        text = str(item)
        # 게이트의 잎은 `<무엇이 다른가>: <좌표>` 모양이다. 뒷부분이 이름을 가리킨다.
        leaves.append(where_leaf(text.split(":", 1)[1] if ":" in text else text))
    return [leaf for leaf in leaves if leaf]


def classify_closure(violation: dict, prev_doc: dict, curr_doc: dict,
                     gates: dict | None) -> tuple[str, str, bool]:
    """닫힌 위반을 코드로 닫은 것과 계약으로 닫은 것으로 가른다.

    계약 판본이 그대로면 볼 것이 없으므로 코드다. 판본이 움직였으면 그 위반의 좌표가 새 판본에 남아 있는지를
    본다. 판정 지점이 사라졌거나 필드가 없어졌거나 필수가 아니게 됐으면 계약이 위반을 없앤 것이다.

    세 번째 값은 그 판정이 두 판본을 실제로 대조해 나왔는지다. 판본 하나를 읽지 못하면 대조가 성립하지 않는데,
    그때 `code`를 사실로 적으면 요구를 지워 없앤 위반이 고친 것으로 집계된다. 판정은 보수적으로 `code`로 두고
    확인하지 못했다는 것을 함께 남긴다.
    """
    if prev_doc == curr_doc:
        return "code", "계약 판본이 앞 iteration과 같다", True
    if not prev_doc or not curr_doc:
        missing = "앞 판본" if not prev_doc else "이번 판본"
        return "code", f"{missing}을 읽지 못해 계약으로 닫혔는지 확인하지 못했다", False

    before = demand_index(prev_doc, gates)
    after = demand_index(curr_doc, gates)

    point = violation.get("point")
    if point and point in before["points"] and point not in after["points"]:
        return "contract", f"판정 지점 {point}이 새 판본에서 사라졌다", True

    for leaf in violation_leaves(violation):
        if leaf in before["properties"] and leaf not in after["properties"]:
            return "contract", f"{leaf}이 새 판본의 스키마에서 사라졌다", True
        if leaf in before["required"] and leaf not in after["required"]:
            return "contract", f"{leaf}이 새 판본에서 필수가 아니게 됐다", True

    return "code", "위반의 좌표와 접힌 잎이 새 판본에도 그대로 있다", True


# ── 위반 id ─────────────────────────────────────────────────────────────────

def violation_id(finding: dict) -> str:
    """규칙 id와 판정 지점과 좌표를 해시한다. `detail`은 개수와 목록을 담아 흔들리므로 넣지 않는다."""
    seed = "|".join([
        str(finding.get("rule") or ""),
        str(finding.get("point") or ""),
        str(finding.get("where") or ""),
    ])
    return "v_" + hashlib.sha256(seed.encode("utf-8")).hexdigest()[:6]


def rule_card(rules: dict, rule_id: str) -> dict:
    """규칙 카드 하나. 뜻과 `family`가 여기서 온다."""
    for card in ((rules or {}).get("rules") or []):
        if card.get("id") == rule_id:
            return card if isinstance(card, dict) else {}
    return {}


def rule_meaning(rules: dict, rule_id: str) -> str:
    return str(rule_card(rules, rule_id).get("meaning") or "")


def label_meaning(rules: dict, label: str) -> str:
    """라벨의 뜻. 어휘는 게이트가 닫아 두고 뜻은 규칙 카드 파일의 `labels`가 고정한다."""
    for card in ((rules or {}).get("labels") or []):
        if isinstance(card, dict) and card.get("id") == label:
            return str(card.get("meaning") or "")
    return ""


def _load_rules() -> dict:
    """규칙 카드. `_gates`와 같은 이유로 두 가지 이름으로 찾는다."""
    import importlib

    for name in ("gates", "pipeline.gates"):
        try:
            module = importlib.import_module(name)
        except ImportError:
            continue
        try:
            return module.load_rules()
        except Exception:  # noqa: BLE001 — 카드를 못 읽어도 원장은 돈다
            return {}
    return {}


# ── 초안 두 판본의 차이 ──────────────────────────────────────────────────────

def normalize_newlines(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n")


def dir_snapshot(output_dir: Path | None) -> dict[str, dict]:
    """작업 폴더의 지금 상태를 훑는다. 매니페스트가 색인만 담으므로 대조 대상은 디스크다.

    파일 목록은 git이 쓰는 목록과 같게 뽑는다. git이 없으면 트리를 걸으며 `.git/`만 뺀다. 파일마다 해시와
    본문을 담는데, 해시는 무엇이 달라졌는지를 정하고 본문은 발췌 diff를 만든다. 텍스트로 읽히지 않거나 너무
    큰 파일은 본문 없이 해시만 담아 추가와 삭제와 수정은 세고 diff는 생략한다.
    """
    snapshot: dict[str, dict] = {}
    if output_dir is None or not Path(output_dir).exists():
        return snapshot
    root = Path(output_dir)

    tracked = git_tracked_paths(root)
    if tracked is not None:
        candidates = [root / rel for rel in tracked]
    else:
        candidates = [p for p in root.rglob("*") if ".git" not in p.relative_to(root).parts]

    for path in sorted(candidates):
        if not path.is_file():
            continue
        data = path.read_bytes()
        text = None
        if len(data) <= MAX_DIFF_BYTES:
            try:
                text = normalize_newlines(data.decode("utf-8"))
            except UnicodeDecodeError:
                text = None
        snapshot[path.relative_to(root).as_posix()] = {
            "sha": hashlib.sha256(data).hexdigest(),
            "text": text,
        }
    return snapshot


def iteration_snapshot(output_dir: Path | None, commit: str) -> dict[str, dict]:
    """그 iteration의 작업 폴더 상태. 커밋이 있으면 그 커밋의 트리를, 없으면 디스크를 읽는다.

    runner는 iteration마다 작업 폴더 전부를 커밋하므로 그 트리가 곧 그 iteration이다. 디스크를 읽으면 두 가지가
    틀린다. 남은 산출물로 원장을 처음부터 다시 만들 때는 작업 폴더가 마지막 iteration에 서 있어 앞 iteration이
    뒤에 생긴 파일을 보고, 체크아웃이 줄바꿈을 바꾸는 환경에서는 같은 파일도 커밋과 디스크의 해시가 달라
    손대지 않은 파일이 전부 바뀐 것으로 잡힌다. 그래서 한 원장 안의 해시는 모두 커밋에서 낸다.
    """
    if not commit or output_dir is None:
        return dir_snapshot(output_dir)
    wanted = _git(output_dir, "rev-parse", "--verify", f"{commit}^{{commit}}")
    if wanted is None:
        return dir_snapshot(output_dir)
    listing = _git(output_dir, "ls-tree", "-r", "-z", wanted.strip().decode("ascii", "replace"))
    if listing is None:
        return dir_snapshot(output_dir)

    snapshot: dict[str, dict] = {}
    for record in listing.decode("utf-8", "replace").split("\0"):
        if "\t" not in record:
            continue
        head_part, rel = record.split("\t", 1)
        parts = head_part.split()
        if len(parts) < 3 or parts[1] != "blob":
            continue
        # 경로가 아니라 blob 해시로 읽는다. `git_show`와 같은 까닭으로 윈도우의 경로 길이 한도를 피한다.
        data = _git(output_dir, "cat-file", "blob", parts[2])
        if data is None:
            continue
        text = None
        if len(data) <= MAX_DIFF_BYTES:
            try:
                text = normalize_newlines(data.decode("utf-8"))
            except UnicodeDecodeError:
                text = None
        snapshot[rel] = {"sha": hashlib.sha256(data).hexdigest(), "text": text}
    return snapshot


def generator_touched(snapshot: dict[str, dict], changes: dict[str, dict],
                      skeleton_paths: set[str] | None, has_prev: bool = False) -> set[str]:
    """이 iteration에 생성 단계가 놓았거나 건드린 파일들. 색인과 맞댈 상대다.

    색인은 그 iteration에 쓰이거나 고쳐진 것의 목록이다. 첫 iteration에는 스켈레톤 커밋에 없던 파일 전부가
    그 목록이고, 뒤의 iteration에는 앞 iteration 대비 달라진 파일만이다. 뒤의 iteration에서도 스켈레톤 밖의
    파일 전부를 맞대면, 앞에서 이미 신고했고 이번에 건드리지 않은 파일이 모두 "색인에 없다"로 올라와 기록이
    신호가 되지 못한다. 처음부터 있고 건드리지 않은 스켈레톤 사본도 어긋남이 아니다. git이 없으면 스켈레톤
    목록을 알 수 없으므로 이번에 달라진 것만 센다.
    """
    touched = set(changes)
    if not has_prev and skeleton_paths is not None:
        touched |= set(snapshot) - skeleton_paths
    return touched & set(snapshot)


def hashes_of(snapshot: dict[str, dict]) -> dict[str, str]:
    """다음 iteration이 앞 상태를 알아보게 원장에 남기는 색인. git 없이도 변경이 나오게 하는 장치다."""
    return {path: entry["sha"] for path, entry in snapshot.items()}


def file_changes(prev_hashes: dict[str, str], curr: dict[str, dict],
                 fetch_old_text=None) -> dict[str, dict]:
    """파일별로 무엇이 달라졌는지 낸다. 없어진 파일과 새 파일도 변경으로 센다.

    앞 상태는 해시 색인으로만 안다. 그래서 무엇이 달라졌는지는 git 없이 나오고, 옛 본문은 달라진 자리에서만
    `fetch_old_text`로 꺼낸다. 꺼내지 못하면 그 파일은 발췌 없이 변경만 센다.
    """
    changed: dict[str, dict] = {}
    for path in sorted(set(prev_hashes) | set(curr)):
        before_sha = prev_hashes.get(path)
        after = curr.get(path)
        if before_sha is not None and after is not None and before_sha == after["sha"]:
            continue
        kind = "added" if before_sha is None else "removed" if after is None else "modified"

        old_text = None
        if before_sha is not None and fetch_old_text is not None:
            old_text = fetch_old_text(path)
        new_text = (after or {}).get("text")

        if kind == "added" and new_text is not None:
            diff = list(_unified("", new_text, path))
        elif before_sha is not None and old_text is None:
            diff = [f"# {path}: 앞 본문을 가져오지 못해 발췌 없이 변경만 센다 ({kind})"]
        elif kind == "removed":
            diff = list(_unified(old_text or "", "", path))
        elif new_text is None:
            diff = [f"# {path}: 텍스트로 대조하지 않는 파일이다 ({kind})"]
        else:
            diff = list(_unified(old_text or "", new_text, path))
        changed[path] = {"kind": kind, "diff": diff}
    return changed


def _unified(before: str, after: str, path: str):
    return difflib.unified_diff(
        before.splitlines(), after.splitlines(),
        fromfile=f"a/{path}", tofile=f"b/{path}", lineterm="", n=1,
    )


def diff_excerpt(entries: list[dict]) -> str:
    lines: list[str] = []
    for entry in entries:
        lines.extend(entry["diff"])
        if len(lines) >= DIFF_EXCERPT_LINES:
            break
    if len(lines) > DIFF_EXCERPT_LINES:
        return "\n".join(lines[:DIFF_EXCERPT_LINES] + [f"... ({len(lines) - DIFF_EXCERPT_LINES}줄 더)"])
    return "\n".join(lines)


# 좌표에서 구조를 나타내는 조각. 이름이 아니라 문서의 뼈대이므로 좌표를 맞댈 때 뺀다.
STRUCTURAL = frozenset({
    "components", "schemas", "properties", "required", "items", "paths", "responses", "content",
    "schema", "requestBody", "parameters", "headers", "application", "json", "allOf", "anyOf", "oneOf",
})

# 좌표로 볼 수 있는 모양. 이름 하나는 좌표가 아니다 — 점이나 슬래시나 콜론으로 자리가 한정되어야 한다.
COORDINATE = re.compile(
    r"#?(?:/[A-Za-z0-9_~\-]+){2,}"                      # JSON 포인터
    r"|[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+"  # 점으로 이은 경로
    r"|[A-Za-z_][A-Za-z0-9_]*:[A-Za-z0-9_]+"            # operationId:상태 또는 operationId:CODE
)


def place_of(raw: str) -> tuple[str, ...]:
    """좌표 하나를 맞댈 수 있는 모양으로 줄인다. 뼈대를 뺀 뒤 마지막 두 조각이다.

    `#/components/schemas/Alert/properties/lastCheckedAt`과 `Alert.lastCheckedAt`이 같은 자리를 가리키므로
    같은 값으로 줄여야 한다. 마지막 두 조각까지만 보는 이유는 앞쪽이 문서 구조에 따라 달라지기 때문이다.
    """
    text = str(raw or "").strip().lstrip("#")
    segments = [s for s in re.split(r"[/.:\[\]()]+", text) if s]
    segments = [s.replace("~1", "/").replace("~0", "~") for s in segments]
    meaningful = [s for s in segments if s not in STRUCTURAL and not s.isdigit()]
    if not meaningful:
        return ()
    return tuple(meaningful[-2:])


def coordinate_places(text: str) -> set[tuple[str, ...]]:
    """문장에서 좌표만 뽑아 맞댈 모양으로 줄인다. 한정되지 않은 이름은 버린다."""
    found = set()
    for match in COORDINATE.findall(str(text or "")):
        place = place_of(match)
        if place:
            found.add(place)
    return found


def finding_places(finding: dict) -> set[tuple[str, ...]]:
    """게이트 항목이 가리키는 자리. 좌표와 접힌 잎에서 뽑는다."""
    found = set()
    where = str(finding.get("where") or "")
    if where:
        place = place_of(where)
        if place:
            found.add(place)
    for item in (finding.get("evidence") or []):
        text = str(item)
        tail = text.split(":", 1)[1] if ":" in text else text
        place = place_of(tail)
        if place:
            found.add(place)
    return found


def violation_tokens(violation: dict) -> set[str]:
    """위반이 가리키는 이름들. 파일 변경이 그 위반과 관계있는지 보는 데 쓴다.

    접힌 잎도 넣는다. 스키마 하나의 차이가 한 건으로 접히면 좌표는 부모 이름뿐이라, 잎을 빼면 그 필드를 고친
    파일이 무관한 변경으로 떨어진다.
    """
    point = str(violation.get("point") or "")
    tokens = set(TOKEN.findall(point.split(":")[0]))
    tokens |= set(TOKEN.findall(str(violation.get("where") or "")))
    for leaf in violation_leaves(violation):
        tokens |= set(TOKEN.findall(leaf))
    return {t for t in tokens if len(t) > 2}


def touches(path: str, entry: dict, tokens: set[str]) -> bool:
    if not tokens:
        return False
    text = path + "\n" + "\n".join(line for line in entry["diff"] if line[:1] in ("+", "-"))
    return any(token in text for token in tokens)


# ── 계약 변경의 셈 ───────────────────────────────────────────────────────────

def is_uncompared(line) -> bool:
    """대조기가 이 자리를 판정하지 못했다고 적은 줄인가."""
    text = str(line)
    return any(mark in text for mark in UNCOMPARED_MARKS)


def split_comparator_lines(lines: list) -> tuple[list[str], list[str]]:
    """대조기가 낸 줄을 깨는 변경과 대조하지 못한 자리로 가른다.

    참조를 풀지 못한 자리는 계약이 깨졌다는 말이 아니라 대조기가 그 자리를 보지 못했다는 말이다. 섞어 세면
    리포트의 "깨는 변경 N건"에 미판정이 들어가 사람이 계약이 깨졌다고 읽는다.
    """
    breaking, uncompared = [], []
    for line in lines:
        (uncompared if is_uncompared(line) else breaking).append(str(line))
    return breaking, uncompared


def _empty_diff(available: bool, error: str | None = None) -> dict:
    return {"pointers": [], "breaking": [], "uncompared": [], "promise_lines": [],
            "points_gone": [], "points_born": [], "available": available, "error": error}


def contract_diff(prev_doc: dict, curr_doc: dict, gates: dict | None) -> dict:
    """판본 둘 사이의 차이를 셈으로 낸다.

    달라진 좌표, 깨는 변경, 대조하지 못한 자리, 판정 지점의 출입이다. 깨는 변경과 대조하지 못한 자리를 가르는
    이유는 뒤의 것이 미판정이기 때문이다.

    대조기가 터지면 그 예외를 여기서 받는다. 원장은 기록 장치이므로 그 실패가 run을 죽이면 안 되고, 그렇다고
    조용히 0건으로 넘기면 계약이 움직이지 않은 것처럼 보인다. 그래서 셈을 비우고 실패를 들고 나가 기록으로 남긴다.
    """
    if gates is None:
        return _empty_diff(False)
    try:
        pointers = [{"kind": kind, "pointer": pointer}
                    for kind, pointer in gates["diff_pointers"](prev_doc, curr_doc)]
        lines: list = []
        gates["compare_http"](prev_doc, curr_doc, lines)
        breaking, uncompared = split_comparator_lines(lines)
        before, after = set(gates["points_of"](prev_doc)), set(gates["points_of"](curr_doc))
        lost = promise_reductions(prev_doc, curr_doc, gates)
    except Exception as failure:  # noqa: BLE001 — 대조기의 어떤 실패도 원장을 멈추게 하지 않는다
        return _empty_diff(True, f"{type(failure).__name__}: {failure}")
    return {
        "pointers": pointers,
        "breaking": breaking,
        "uncompared": uncompared,
        "promise_lines": lost,
        "points_gone": sorted(before - after),
        "points_born": sorted(after - before),
        "available": True,
        "error": None,
    }


def promise_reductions(prev_doc: dict, curr_doc: dict, gates: dict | None) -> list[str]:
    """약속이 줄어든 자리. 판정 지점과 필수와 에러 코드 쌍과 산문 슬롯 가운데 하나라도 줄면 약속 변경이다.

    표현만 바꾼 변경과 약속을 줄인 변경을 가르는 데 쓴다. 이름을 고치거나 설명을 다시 쓰는 것은 계약이 무엇을
    약속하는지를 바꾸지 않으므로 사람이 나중에 봐도 되고, 약속이 줄어든 자리는 먼저 봐야 한다.

    판정 지점의 좌표 규약이 `operationId:status`와 `operationId:CODE`이므로 에러 코드 쌍은 판정 지점 안에서
    함께 세진다. 산문 슬롯은 추출기가 다른 조각의 것이라 없으면 그 항목만 빠진다.
    """
    if not prev_doc or not curr_doc:
        return []
    found: list[str] = []
    if gates is not None:
        for point in sorted(set(gates["points_of"](prev_doc)) - set(gates["points_of"](curr_doc))):
            found.append(f"판정 지점이 줄었다 {point}")
    gone = demand_index(prev_doc, gates)["required"] - demand_index(curr_doc, gates)["required"]
    for name in sorted(gone):
        found.append(f"필수가 줄었다 {name}")
    before, after = _prose_slot_count(prev_doc), _prose_slot_count(curr_doc)
    if before is not None and after is not None and after < before:
        found.append(f"산문 슬롯이 {before}개에서 {after}개로 줄었다")
    return found


def _prose_slot_count(doc: dict) -> int | None:
    """산문 슬롯의 개수. 추출기는 다른 조각이 소유하므로 없으면 None을 돌려 그 항목을 빼게 한다."""
    import importlib

    prose_slots = None
    for name in ("tools.prose_slots", "pipeline.tools.prose_slots"):
        try:
            prose_slots = importlib.import_module(name)
            break
        except ImportError:
            continue
    if prose_slots is None:
        return None
    for name in ("extract_prose_slots", "extract"):
        extractor = getattr(prose_slots, name, None)
        if callable(extractor):
            try:
                slots = extractor(doc)
            except Exception:  # noqa: BLE001 — 셈 하나가 없어도 원장은 돈다
                return None
            return len(slots) if isinstance(slots, list) else None
    return None


def record_tokens(record: dict, pointers: list[dict], prev_doc: dict, curr_doc: dict) -> set[str]:
    """기록이 덮는 좌표의 이름들. 좌표 자신의 토큰과 그 자리에 있던 값을 함께 담는다.

    필수 목록에서 이름을 뺀 변경은 좌표가 `/components/schemas/Alert/required/3`으로 나오고 정작 필드 이름은
    그 자리의 값에 있다. 값을 함께 담지 않으면 깨는 변경을 그 기록에 붙일 수 없다.
    """
    target = normalize_pointer(record.get("target"))
    tokens = set(TOKEN.findall(target))
    for item in pointers:
        pointer = item["pointer"]
        if not (pointer == target or pointer.startswith(target + "/") or target.startswith(pointer + "/")):
            continue
        tokens |= set(TOKEN.findall(pointer))
        for doc in (prev_doc, curr_doc):
            value = pointer_value(doc, pointer)
            if isinstance(value, str):
                tokens |= set(TOKEN.findall(value))
    return {t for t in tokens if len(t) > 2}


def normalize_pointer(target) -> str:
    value = str(target or "").strip()
    if value.startswith("#"):
        value = value[1:]
    if value and not value.startswith("/"):
        value = "/" + value
    return value


def pointer_value(doc, pointer: str):
    node = doc
    for token in pointer.split("/")[1:]:
        token = token.replace("~1", "/").replace("~0", "~")
        if isinstance(node, dict):
            if token not in node:
                return None
            node = node[token]
        elif isinstance(node, list):
            if not token.isdigit() or int(token) >= len(node):
                return None
            node = node[int(token)]
        else:
            return None
    return node


# ── 원장 갱신 ───────────────────────────────────────────────────────────────

def _blank_ledger(run_dir: Path) -> dict:
    return {
        "run_id": run_dir.name,
        "updated_at": _now(),
        "iterations": [],
        "violations": [],
        "decisions": [],
        "contract_changes": [],
        "observations": [],
        "impact": {},
        "pass_bar": {},
        "disagreements": {
            "claim_mismatch": [],
            "silent_reversal": [],
            "unjustified_proposal": [],
            "axis_disagreement": [],
            "machine_checkable_restatement": [],
        },
    }


def _index(items: list[dict], key: str) -> dict[str, dict]:
    return {str(item.get(key)): item for item in items if item.get(key)}


def update_ledger(run_dir: Path, iteration: str, payload: dict) -> dict:
    """iteration 하나를 원장에 반영하고 갱신된 원장을 돌려준다.

    payload는 runner가 합산 자리에서 넘기는 것이다. draft는 경로로 오므로 여기서 읽는다.
    """
    run_dir = Path(run_dir)
    ledger_path = run_dir / LEDGER_NAME
    ledger = _read_json(ledger_path) or _blank_ledger(run_dir)
    for key, empty in _blank_ledger(run_dir).items():
        ledger.setdefault(key, empty)
    if isinstance(ledger.get("disagreements"), dict):
        for key, empty in _blank_ledger(run_dir)["disagreements"].items():
            ledger["disagreements"].setdefault(key, empty)

    gates = _gates()
    rules = _load_rules()

    draft_rel = str(payload.get("draft") or "")
    draft = _read_json(run_dir / _safe_rel(draft_rel)) if draft_rel else {}
    gate = payload.get("gate") or {}

    prev_entry = ledger["iterations"][-1] if ledger["iterations"] else None
    prev_draft = _read_json(run_dir / prev_entry["draft"]) if prev_entry and prev_entry.get("draft") else {}
    # 작업 폴더와 커밋 해시는 runner가 payload에 실어 준다. 없으면 규약과 draft에서 되짚는다.
    output_dir = output_dir_of(run_dir, payload.get("output"))
    commit = str(payload.get("commit") or "").strip() or commit_of(draft)
    prev_commit = str((prev_entry or {}).get("commit") or "")

    # ── 계약 판본. 이번 판본, 앞 판본, 기준선, 원본
    version = draft.get("contract_version") or {}
    curr_doc, curr_source = contract_doc(run_dir, version.get("path", ""), commit)
    prev_doc, prev_source = contract_doc(run_dir, version.get("from", ""), prev_commit)
    baseline_file = _first_in(run_dir / "baseline")
    origin_file = _first_in(run_dir / "origin")

    step_diff = contract_diff(prev_doc, curr_doc, gates)
    baseline_diff = contract_diff(_read_contract(baseline_file) if baseline_file else {}, curr_doc, gates)
    origin_diff = contract_diff(_read_contract(origin_file) if origin_file else {}, curr_doc, gates)

    # 코드 변경은 앞 iteration의 해시 색인과 지금 `output/`을 비교해 낸다. 옛 본문은 앞 커밋에서 꺼낸다.
    # 계약 판본 파일은 거기서 뺀다. 판본이 움직인 것은 2절과 3절이 좌표별로 펴므로, 여기에 두면 "확인된 수정"이
    # 계약 전문으로 덮여 코드가 실제로 달라졌는지가 가려진다.
    snapshot = iteration_snapshot(output_dir, commit)
    contract_paths = contract_paths_of(draft) | contract_paths_of(prev_draft)
    prev_hashes = dict((prev_entry or {}).get("file_hashes") or {})
    all_changes = (
        file_changes(prev_hashes, snapshot, lambda rel: git_show(output_dir, prev_commit, rel))
        if prev_entry else {}
    )
    changed_files = {path: entry for path, entry in all_changes.items() if path not in contract_paths}

    # ── 1절. 위반
    open_before = [v for v in ledger["violations"] if v.get("status") in (STATUS_OPEN, STATUS_REGRESSED)]
    _update_violations(
        ledger=ledger,
        iteration=iteration,
        gate=gate,
        draft=draft,
        rules=rules,
        prev_doc=prev_doc,
        curr_doc=curr_doc,
        gates=gates,
        changed_files=changed_files,
    )

    # ── 2절. 결정
    reversals = _update_decisions(ledger, iteration, draft, prev_draft)

    # ── 3절. 계약 변경
    _update_contract_changes(ledger, iteration, draft, step_diff, prev_doc, curr_doc)

    # ── 4절. 기록
    _record_observations(
        ledger=ledger,
        iteration=iteration,
        gate=gate,
        step_diff=step_diff,
        baseline_diff=baseline_diff,
        origin_diff=origin_diff,
        changed_files=changed_files,
        open_before=open_before,
        payload=payload,
        declared_paths={str(e.get("path")) for e in (draft.get("files") or []) if isinstance(e, dict) and e.get("path")},
        disk_paths=set(snapshot),
        touched_paths=generator_touched(snapshot, all_changes, git_skeleton_paths(output_dir),
                                        has_prev=prev_entry is not None),
        rules=rules,
        commit=commit,
        prev_commit=prev_commit,
        has_prev=prev_entry is not None,
    )

    # ── 5절. 영향
    ledger["impact"] = _impact(
        iteration=iteration,
        payload=payload,
        version=version,
        step_diff=step_diff,
        baseline_diff=baseline_diff,
        origin_diff=origin_diff,
        baseline_file=baseline_file,
        origin_file=origin_file,
        ledger=ledger,
    )

    # ── 6절. 주장과 판정의 불일치
    _record_disagreements(ledger, iteration, payload, draft, gate, reversals)

    ledger["iterations"] = [e for e in ledger["iterations"] if e.get("iteration") != iteration]
    ledger["iterations"].append({
        "iteration": iteration,
        "status": payload.get("status"),
        "draft": draft_rel,
        "output_dir": output_dir.relative_to(run_dir).as_posix() if output_dir.is_relative_to(run_dir) else str(output_dir),
        "commit": commit,
        "declared_files": len(draft.get("files") or []),
        "files_on_disk": len(snapshot),
        # 다음 iteration이 앞 상태를 알아보는 색인. 작업 폴더를 제자리에서 고치므로 이것이 유일한 앞 상태다
        "file_hashes": hashes_of(snapshot),
        "contract": {"path": version.get("path"), "from": version.get("from")},
        "contract_source": curr_source or None,
        "prev_contract_source": prev_source or None,
        "gate": {
            "g0": gate.get("g0"),
            "g1": gate.get("g1"),
            "changes": gate.get("changes"),
            "violations": len(gate.get("violations") or []),
            "observations": len(gate.get("observations") or []),
        },
        "eval": _eval_summary(payload.get("eval")),
        "unmeasured_axes": list(payload.get("unmeasured_axes") or []),
        # v0의 합격선. 판정했는가와 위반이 없는가를 갈라 둔다
        "machine_verdict": machine_verdict(run_dir, payload),
        "decision_risk": payload.get("decision_risk"),
        "skipped_reason": payload.get("skipped_reason"),
        "gate_errors": list(payload.get("gate_errors") or []),
        "rubric_errors": list(payload.get("rubric_errors") or []),
    })
    ledger["iterations"].sort(key=lambda e: str(e.get("iteration")))
    ledger["pass_bar"] = pass_bar_of(ledger["iterations"])
    ledger["updated_at"] = _now()

    ledger_path.parent.mkdir(parents=True, exist_ok=True)
    ledger_path.write_text(json.dumps(ledger, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return ledger


JUDGED_STATUSES = ("PASS", "REJECT")
# 게이트의 키와 사람이 읽는 이름, 그리고 그 이름에 붙는 주격 조사.
GATE_NAMES = (("g0", "G0", "이"), ("g1", "G1", "이"), ("changes", "판본 대조", "가"))


def machine_verdict(run_dir: Path, payload: dict) -> dict:
    """v0의 합격선. 두 문장을 갈라 판정한다.

    합격선은 최종 판정 PASS가 아니라 기계 판정이 깨끗한 것이다. 계약 적합성은 점수보다 결정적 대조가 안전하고,
    Eval이 문턱에 못 미쳐 FAILED로 끝나도 게이트가 깨끗한 iteration이 나왔다면 이 하네스는 제 일을 한 것이다.

    두 문장을 갈라 두는 이유는 뒤의 문장만 적으면 게이트를 부수는 것이 게이트를 통과하는 가장 쉬운 길이 되기
    때문이다. 세 판정이 모두 `SKIPPED`인데 위반 목록이 비어 있는 run은 실제로 있었고, 뒤의 문장만으로는 그것이
    합격으로 읽힌다. 그래서 판정하지 못한 것을 위반 0건과 같은 칸에 넣지 않는다.

    - **판정했다** — G0·G1·판본 대조가 `SKIPPED`도 `ERROR`도 아니고, 추출 스펙이 실물로 나왔고,
      Eval이 재지 못한 축이 없다.
    - **그리고 위반이 없다** — G0가 PASS, G1 위반 0건, 계약 변경 위반 0건.

    모르는 값을 참으로 가정하지 않는다. `unmeasured_axes`가 payload에 아예 없으면 "재지 못한 축이 없다"가
    아니라 "말할 수 없다"이고, 그 자리는 합격선을 넘지 못한 것으로 센다.
    """
    gate = payload.get("gate") or {}

    judged_gaps: list[str] = []
    for key, name, particle in GATE_NAMES:
        status = str(gate.get(key) or "")
        if status not in JUDGED_STATUSES:
            judged_gaps.append(f"{name}{particle} {status or '없음'}이다 — 판정되지 않았다")

    if "derived_spec" not in gate:
        judged_gaps.append("추출 스펙의 자리가 payload에 없다")
    elif not gate.get("derived_spec"):
        judged_gaps.append("추출 스펙이 나오지 않았다")
    else:
        spec = Path(str(gate["derived_spec"]))
        if not spec.is_absolute():
            spec = Path(run_dir) / spec
        if not spec.exists():
            judged_gaps.append(f"추출 스펙 파일이 실물로 없다: {gate['derived_spec']}")

    if payload.get("eval") is None:
        judged_gaps.append("Eval이 돌지 않아 축을 쟀는지 말할 수 없다")
    elif "unmeasured_axes" not in payload:
        judged_gaps.append("Eval이 재지 못한 축의 목록이 payload에 없다 — 비어 있다고 가정하지 않는다")
    elif payload.get("unmeasured_axes"):
        judged_gaps.append(f"Eval이 재지 못한 축이 있다: {list(payload['unmeasured_axes'])}")

    violation_gaps: list[str] = []
    for key, name, particle in GATE_NAMES:
        status = str(gate.get(key) or "")
        if status == "PASS":
            continue
        if status in JUDGED_STATUSES:
            violation_gaps.append(f"{name}{particle} {status}다 — 위반이 있다")
        else:
            violation_gaps.append(f"{name}{particle} 판정되지 않아 위반 0건이라고 말할 수 없다")

    return {
        "judged": not judged_gaps,
        "judged_gaps": judged_gaps,
        "no_violation": not violation_gaps,
        "violation_gaps": violation_gaps,
        "clean": not judged_gaps and not violation_gaps,
    }


def pass_bar_of(iterations: list[dict]) -> dict:
    """run 전체의 한 줄. 기계적으로 깨끗한 iteration이 있었는지, 없으면 무엇에 걸렸는지."""
    clean, judged_only, blocked = [], [], {}
    for entry in iterations:
        verdict = entry.get("machine_verdict") or {}
        name = str(entry.get("iteration"))
        if verdict.get("clean"):
            clean.append(name)
            continue
        if verdict.get("judged"):
            judged_only.append(name)
        blocked[name] = list(verdict.get("judged_gaps") or []) + list(verdict.get("violation_gaps") or [])
    return {"clean_iterations": clean, "judged_but_violating": judged_only, "blocked": blocked}


def _eval_summary(eval_artifact) -> dict | None:
    if not isinstance(eval_artifact, dict):
        return None
    scores = (eval_artifact.get("rubric_scores") or {})
    return {
        "weighted_total": scores.get("weighted_total"),
        "scores": scores.get("scores") or {},
        "axis_rationales": eval_artifact.get("axis_rationales") or {},
    }


# ── 1절. 위반 ───────────────────────────────────────────────────────────────

def _update_violations(ledger, iteration, gate, draft, rules, prev_doc, curr_doc, gates, changed_files) -> None:
    """이번 iteration의 게이트 결과로 위반 절을 갱신한다.

    새로 나온 것은 열고, 이미 열려 있던 것은 그대로 두고, 사라진 것은 닫는다. 닫을 때 코드로 닫았는지
    계약으로 닫았는지를 가른다. 닫힘은 게이트 결과만 줄 수 있으므로 refine의 주장은 닫는 근거가 아니다.
    """
    known = _index(ledger["violations"], "id")
    repairs = {str(r.get("violation_id")): str(r.get("summary") or "")
               for r in (draft.get("repairs") or []) if r.get("violation_id")}

    seen: set[str] = set()
    for finding in (gate.get("violations") or []):
        if not isinstance(finding, dict):
            continue
        vid = violation_id(finding)
        seen.add(vid)
        entry = known.get(vid)
        if entry is None:
            card = rule_card(rules, finding.get("rule"))
            entry = {
                "id": vid,
                "rule": finding.get("rule"),
                # 규칙 id는 게이트가 갈리면 바뀐다. `family`는 그 위 고도의 이름이라 id가 바뀌어도 집계가 이어진다.
                "family": card.get("family") or "",
                "point": finding.get("point"),
                "where": finding.get("where"),
                "why_it_matters": str(card.get("meaning") or "") or str(finding.get("detail") or ""),
                "detail": finding.get("detail"),
                # 라벨은 그 차이가 쓰는 사람에게 어떤 뜻인지다. 판정을 가르지 않고 리포트의 순서만 정한다.
                "label": finding.get("label") or "",
                "label_meaning": label_meaning(rules, finding.get("label") or ""),
                # 접힌 잎. 한 건으로 보이는 위반이 몇 자리를 덮는지가 여기에 있다.
                "evidence": list(finding.get("evidence") or []),
                "first_seen_iteration": iteration,
                "last_seen_iteration": iteration,
                "closed_in_iteration": None,
                "closed_by": None,
                "closed_because": None,
                "closed_by_verified": None,
                "claimed_fix": None,
                "verified_fix": None,
                "introduced_by": None,
                "recurrence": 0,
                "status": STATUS_OPEN,
            }
            ledger["violations"].append(entry)
            known[vid] = entry
            continue

        # 같은 위반이라도 접힌 잎과 라벨은 iteration마다 달라질 수 있다. 마지막으로 본 것을 들고 간다.
        entry["detail"] = finding.get("detail")
        entry["label"] = finding.get("label") or entry.get("label") or ""
        entry["label_meaning"] = label_meaning(rules, entry["label"]) or entry.get("label_meaning") or ""
        entry["evidence"] = list(finding.get("evidence") or [])
        entry["last_seen_iteration"] = iteration
        if entry["status"] in (STATUS_CLOSED,):
            # 닫혔던 id가 다시 열렸다. 한 번은 regressed, 두 번이면 stuck이다.
            entry["recurrence"] = int(entry.get("recurrence") or 0) + 1
            entry["status"] = STATUS_STUCK if entry["recurrence"] >= 2 else STATUS_REGRESSED
            entry["closed_in_iteration"] = None
            entry["closed_by"] = None
            entry["closed_because"] = None
            entry["closed_by_verified"] = None
            entry["verified_fix"] = None
        elif entry["status"] == STATUS_REGRESSED:
            entry["status"] = STATUS_REGRESSED

    for entry in ledger["violations"]:
        if entry["id"] in seen:
            continue
        if entry["status"] not in (STATUS_OPEN, STATUS_REGRESSED):
            continue
        closed_by, because, verified = classify_closure(entry, prev_doc, curr_doc, gates)
        entry["status"] = STATUS_CLOSED
        entry["closed_in_iteration"] = iteration
        entry["closed_by"] = closed_by
        entry["closed_because"] = because
        entry["closed_by_verified"] = verified
        entry["claimed_fix"] = repairs.get(entry["id"])
        entry["verified_fix"] = _verified_fix(entry, changed_files)


def _verified_fix(violation: dict, changed_files: dict[str, dict]) -> dict:
    """작업 폴더가 앞 iteration과 달라진 자리에서 이 위반에 닿는 것만 고른다. refine의 주장과 따로 둔다."""
    tokens = violation_tokens(violation)
    related = [(path, entry) for path, entry in changed_files.items() if touches(path, entry, tokens)]
    if not related:
        return {"files": [], "diff": "", "note": "이 위반의 좌표를 언급하는 파일 변경이 없다"}
    return {
        "files": [path for path, _ in related],
        "diff": diff_excerpt([entry for _, entry in related]),
        "note": "",
    }


# ── 2절. 결정 ───────────────────────────────────────────────────────────────

def _update_decisions(ledger, iteration, draft, prev_draft) -> list[dict]:
    """결정 절을 갱신하고 말없는 되돌림을 찾아 돌려준다.

    결정은 모델이 붙인 id를 그대로 이어 쓴다. 서 있는 결정이 `supersedes` 없이 사라졌거나 고른 것이
    달라졌으면 되돌림이다.
    """
    known = _index(ledger["decisions"], "id")
    current = [d for d in (draft.get("decisions") or []) if isinstance(d, dict) and d.get("id")]
    current_index = _index(current, "id")
    superseded = {str(d.get("supersedes")) for d in current if d.get("supersedes")}

    reversals: list[dict] = []
    previous = [d for d in (prev_draft.get("decisions") or []) if isinstance(d, dict) and d.get("id")]
    for before in previous:
        did = str(before["id"])
        after = current_index.get(did)
        if after is None and did not in superseded:
            reversals.append({
                "decision_id": did,
                "iteration": iteration,
                "detail": f"서 있던 결정 {did}이 supersedes 없이 사라졌다",
            })
        elif after is not None and after.get("chosen") != before.get("chosen") and not after.get("supersedes"):
            reversals.append({
                "decision_id": did,
                "iteration": iteration,
                "detail": f"결정 {did}의 고른 것이 supersedes 없이 달라졌다: "
                          f"{before.get('chosen')!r} → {after.get('chosen')!r}",
            })

    for decision in current:
        did = str(decision["id"])
        entry = known.get(did)
        if entry is None:
            entry = {"id": did, "first_seen_iteration": iteration}
            ledger["decisions"].append(entry)
            known[did] = entry
        entry.update({
            "point": decision.get("point"),
            "where": decision.get("where"),
            "question": decision.get("question"),
            "considered": decision.get("considered") or [],
            "chosen": decision.get("chosen"),
            "basis": decision.get("basis"),
            "rationale": decision.get("rationale"),
            "confidence": decision.get("confidence"),
            "blast_radius": decision.get("blast_radius") or [],
            "reversal_cost": decision.get("reversal_cost"),
            "supersedes": decision.get("supersedes"),
            "last_seen_iteration": iteration,
            "status": "standing",
        })

    for entry in ledger["decisions"]:
        did = str(entry.get("id"))
        if did in current_index:
            continue
        if did in superseded:
            entry["status"] = "superseded"
            entry["superseded_in_iteration"] = iteration
        elif entry.get("status") == "standing":
            entry["status"] = "withdrawn"
            entry["withdrawn_in_iteration"] = iteration

    for reversal in reversals:
        entry = known.get(reversal["decision_id"])
        if entry is not None:
            entry["silent_reversal"] = True
    return reversals


# ── 3절. 계약 변경 ──────────────────────────────────────────────────────────

def _update_contract_changes(ledger, iteration, draft, step_diff, prev_doc, curr_doc) -> None:
    """계약 변경 절을 갱신한다. 모델의 호환성 라벨과 대조기의 판정을 나란히 적는다.

    대조하지 못한 줄은 호환성 판정에 쓰지 않는다. 그 자리는 깨졌다고도 안 깨졌다고도 말할 수 없으므로,
    기록에 따로 달아 두고 판정은 대조가 성립한 줄로만 한다.

    변경의 종류도 여기서 붙인다. 판정 장치가 `kind`를 적어 보내면 그것을 쓰고, 없으면 약속이 줄어든 자리를
    그 기록에 붙여 정한다. 붙는 것이 있으면 약속 변경이고 없으면 표현 변경이다.
    """
    known = _index(ledger["contract_changes"], "id")
    breaking = list(step_diff.get("breaking") or [])
    uncompared = list(step_diff.get("uncompared") or [])
    pointers = list(step_diff.get("pointers") or [])
    reductions = list(step_diff.get("promise_lines") or [])

    def attribute(lines: list[str], tokens: set[str]) -> list[str]:
        return [line for line in lines if tokens & {t for t in TOKEN.findall(line) if len(t) > 2}]

    for record in (draft.get("contract_changes") or []):
        if not isinstance(record, dict) or not record.get("id"):
            continue
        cid = str(record["id"])
        tokens = record_tokens(record, pointers, prev_doc, curr_doc)
        attributed = attribute(breaking, tokens)
        unjudged = attribute(uncompared, tokens)
        lost = attribute(reductions, tokens)
        label = str(record.get("compatibility") or "").strip()
        verdict = "breaking" if attributed else "uncompared" if unjudged else "compatible"
        kind = str(record.get("kind") or "").strip() or ("promise" if lost else "representation")
        entry = known.get(cid)
        if entry is None:
            entry = {"id": cid, "first_seen_iteration": iteration}
            ledger["contract_changes"].append(entry)
            known[cid] = entry
        entry.update({
            "target": record.get("target"),
            "action": record.get("action"),
            "what": record.get("what"),
            "why": record.get("why"),
            "basis": record.get("basis"),
            "spec_anchor": record.get("spec_anchor"),
            "compatibility_claimed": label,
            "compatibility_verdict": verdict,
            "breaking_lines": attributed,
            "uncompared_lines": unjudged,
            "kind": kind,
            "promise_lines": lost,
            "migration": record.get("migration"),
            "iteration": record.get("iteration") or iteration,
            "last_seen_iteration": iteration,
        })


# ── 4절. 기록 ───────────────────────────────────────────────────────────────

def _add_observation(ledger, kind, iteration, point, where, detail, extra=None) -> None:
    entry = {"kind": kind, "iteration": iteration, "point": point, "where": where, "detail": detail}
    if extra:
        entry.update({key: value for key, value in extra.items() if value})
    if entry not in ledger["observations"]:
        ledger["observations"].append(entry)


def _record_observations(ledger, iteration, gate, step_diff, baseline_diff, origin_diff,
                         changed_files, open_before, payload, declared_paths, disk_paths,
                         touched_paths, commit, prev_commit, has_prev, rules=None) -> None:
    """기록 절. 위반이 아니라 셈이다. REJECT 사유가 아니고 리포트와 slow loop의 재료다."""
    # 커밋 해시가 없으면 변경은 해시 색인으로 그대로 세고 발췌 diff만 빠진다. 그 사실을 남긴다.
    if not commit:
        _add_observation(ledger, "commit_missing", iteration, None, OUTPUT_DIR,
                         "draft에 커밋 해시가 없다. 이 iteration의 상태를 git에서 되짚을 수 없다")
    if has_prev and not prev_commit:
        _add_observation(ledger, "commit_missing", iteration, None, OUTPUT_DIR,
                         "앞 iteration의 커밋 해시가 없다. 변경은 해시 색인으로 세고 발췌 diff는 뺀다")

    # 색인과 실물이 어긋나는가. 판정은 게이트의 몫이고 원장은 어느 경로에서 어긋났는지만 센다.
    # 대조 상대는 작업 폴더 전체가 아니라 생성기가 놓았거나 건드린 파일이다. 스켈레톤 사본까지 세면
    # 사람이 소유한 파일이 매 iteration 어긋남으로 올라와 기록이 신호가 되지 못한다.
    for path in sorted(declared_paths - disk_paths):
        _add_observation(ledger, "manifest_drift", iteration, None, path, "색인에 적힌 경로가 실물로 없다")
    for path in sorted(touched_paths - declared_paths):
        _add_observation(ledger, "manifest_drift", iteration, None, path,
                         "색인에 없는 파일이 작업 폴더에서 바뀌어 있다")

    # 게이트가 남긴 기록. 위반과 같은 모양이므로 가족과 라벨을 함께 들고 간다. 「좁혔다」 가족처럼 위반이
    # 아니라 기록으로만 나오는 자리가 있어서, 라벨의 뜻을 여기서도 붙여 두지 않으면 리포트가 그것을 설명하지 못한다.
    for finding in (gate.get("observations") or []):
        if isinstance(finding, dict):
            card = rule_card(rules, finding.get("rule"))
            _add_observation(ledger, f"gate:{finding.get('rule')}", iteration,
                             finding.get("point"), finding.get("where"), finding.get("detail"),
                             extra={"family": card.get("family") or "",
                                    "label": finding.get("label") or "",
                                    "label_meaning": label_meaning(rules, finding.get("label") or ""),
                                    "evidence": list(finding.get("evidence") or [])})

    if payload.get("skipped_reason"):
        _add_observation(ledger, "stage_skipped", iteration, None, None, str(payload["skipped_reason"]))
    for name in ("g0", "g1", "changes"):
        if gate.get(name) == "SKIPPED":
            _add_observation(ledger, "gate_unjudged", iteration, None, name,
                             f"{name}가 판정되지 않았다. 미판정은 통과가 아니다")

    # 계약이 얼마나 움직였는지. 깨는 변경과 대조하지 못한 자리를 같은 수에 섞지 않는다
    for kind, label, diff in (("contract_step_diff", "앞 판본 대비", step_diff),
                              ("baseline_diff", "이 run의 기준선 대비", baseline_diff),
                              ("origin_diff", "사람이 확정한 원본 대비 누적으로", origin_diff)):
        if not diff.get("available"):
            continue
        tail = f", 대조하지 못한 자리 {len(diff.get('uncompared') or [])}곳" if diff.get("uncompared") else ""
        _add_observation(ledger, kind, iteration, None, "*",
                         f"{label} 달라진 좌표 {len(diff.get('pointers') or [])}개, "
                         f"깨는 변경 {len(diff.get('breaking') or [])}건{tail}")
        if diff.get("error"):
            _add_observation(ledger, "comparator_failed", iteration, None, "*",
                             f"{label} 대조기가 실패해 이 셈을 내지 못했다: {diff['error']}")

    # 약속이 줄어든 자리 가운데 어느 기록에도 붙지 않은 것. 신고 없이 약속이 줄었다는 뜻이다.
    claimed_reductions = {line for record in ledger["contract_changes"]
                          if record.get("last_seen_iteration") == iteration
                          for line in (record.get("promise_lines") or [])}
    for line in (step_diff.get("promise_lines") or []):
        if line not in claimed_reductions:
            _add_observation(ledger, "promise_reduced", iteration, None, "*",
                             f"어느 기록에도 붙지 않은 약속 축소다: {line}")

    # 대조기가 참조를 풀지 못해 보지 못한 자리. 판정이 아니라 미판정이므로 깨는 변경과 따로 센다.
    # 미판정을 통과로도 위반으로도 읽지 않게 하는 것이 이 기록의 일이다.
    for line in (step_diff.get("uncompared") or []):
        where, _, detail = str(line).partition(": ")
        _add_observation(ledger, "contract_uncompared", iteration, None, where or "*",
                         detail or str(line))

    # 같은 좌표를 몇 번 흔들었는가. 어느 iteration에서 흔들었는지를 적어 두고 그 개수를 센다.
    # 개수만 세면 runner가 같은 iteration을 다시 돌릴 때(게이트 결과는 매 실행 다시 계산된다) 두 번 센다.
    touches_by_pointer = {k: list(v) for k, v in (ledger.get("pointer_touches") or {}).items()}
    for item in (step_diff.get("pointers") or []):
        seen = touches_by_pointer.setdefault(item["pointer"], [])
        if iteration not in seen:
            seen.append(iteration)
    ledger["pointer_touches"] = touches_by_pointer
    for pointer, iterations in sorted(touches_by_pointer.items()):
        if len(iterations) >= 2:
            _add_observation(ledger, "contract_oscillation", iteration, None, pointer,
                             f"같은 계약 좌표를 {len(iterations)}번 바꿨다 (iteration {', '.join(iterations)})")

    # 모델의 호환성 라벨이 대조기 판정과 어긋나는가
    labelled = {str(r.get("compatibility_claimed") or "") for r in ledger["contract_changes"]
                if r.get("last_seen_iteration") == iteration}
    for record in ledger["contract_changes"]:
        if record.get("last_seen_iteration") != iteration:
            continue
        claimed, verdict = record.get("compatibility_claimed"), record.get("compatibility_verdict")
        # 대조하지 못한 자리를 근거로 라벨이 틀렸다고 말하지 않는다. 대조기가 판정을 내지 않았으므로
        # 어긋났다고 할 상대가 없다. 그 자리는 `contract_uncompared`가 따로 센다.
        if claimed and verdict not in ("", None, "uncompared") and claimed != verdict:
            _add_observation(ledger, "mislabeled_change", iteration, None, record.get("target"),
                             f"기록 {record['id']}은 {claimed}이라 적었는데 대조기 판정은 {verdict}다")
    # 대조하지 못한 줄은 여기 쓰지 않는다. 그 자리를 근거로 라벨이 틀렸다고 말할 수 없다.
    if (step_diff.get("breaking") or []) and "breaking" not in labelled:
        _add_observation(ledger, "mislabeled_change", iteration, None, "*",
                         f"대조기는 깨는 변경 {len(step_diff['breaking'])}건을 찾았는데 breaking 라벨이 붙은 기록이 없다")

    # 곁가지 변경. 열린 위반과 무관한 파일을 바꿨는가. 계약 판본 파일은 부르는 쪽에서 이미 빠져 있다
    tokens = set()
    for violation in open_before:
        tokens |= violation_tokens(violation)
    for path, entry in changed_files.items():
        if tokens and touches(path, entry, tokens):
            continue
        _add_observation(ledger, "collateral_change", iteration, None, path,
                         "열린 위반의 좌표를 언급하지 않는 파일이 달라졌다"
                         if tokens else "열린 위반이 없는데 파일이 달라졌다")


# ── 5절. 영향 ───────────────────────────────────────────────────────────────

def _impact(iteration, payload, version, step_diff, baseline_diff, origin_diff,
            baseline_file, origin_file, ledger) -> dict:
    """계약이 움직여 무엇이 달라졌는가. 판정 지점의 출입과 두 분모 대비 diff를 함께 낸다.

    분모마다 깨는 변경과 대조하지 못한 자리를 따로 싣는다. 리포트가 그 둘을 한 수로 합치지 않게 하려면
    원장이 먼저 갈라 두어야 한다.
    """
    closed = [v for v in ledger["violations"] if v.get("status") == "closed"]

    def side(diff: dict) -> dict:
        return {
            "pointers": [i["pointer"] for i in (diff.get("pointers") or [])],
            "breaking": diff.get("breaking") or [],
            "uncompared": diff.get("uncompared") or [],
            "promise_lines": diff.get("promise_lines") or [],
            "error": diff.get("error"),
        }

    return {
        "iteration": iteration,
        "contract_version": {"path": version.get("path"), "from": version.get("from")},
        "baseline_contract": baseline_file.name if baseline_file else None,
        "origin_contract": origin_file.name if origin_file else None,
        "points_gone": baseline_diff.get("points_gone") or [],
        "points_born": baseline_diff.get("points_born") or [],
        "step_diff": side(step_diff),
        "baseline_diff": side(baseline_diff),
        "origin_diff": {**side(origin_diff), "points_gone": origin_diff.get("points_gone") or []},
        "closed_by_code": [v["id"] for v in closed if v.get("closed_by") == "code"],
        "closed_by_contract": [v["id"] for v in closed if v.get("closed_by") == "contract"],
        "decision_risk": payload.get("decision_risk"),
    }


# ── 6절. 주장과 판정의 불일치 ────────────────────────────────────────────────

def _record_disagreements(ledger, iteration, payload, draft, gate, reversals) -> None:
    """모델이 한 말과 장치가 낸 판정이 갈린 자리를 모은다. 넷에 machine_checkable_restatement를 더한다."""
    bucket = ledger["disagreements"]

    def push(key, entry):
        entry = {"iteration": iteration, **entry}
        if entry not in bucket[key]:
            bucket[key].append(entry)

    # claims와 게이트 판정의 대조
    violated_points = {str(v.get("point")) for v in (gate.get("violations") or []) if isinstance(v, dict)}
    for claim in (draft.get("claims") or []):
        if not isinstance(claim, dict):
            continue
        point = str(claim.get("point") or "")
        if point and point in violated_points:
            push("claim_mismatch", {
                "point": point,
                "note": claim.get("note"),
                "detail": "충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다",
            })
        elif gate.get("g1") == "SKIPPED":
            push("claim_mismatch", {
                "point": point,
                "note": claim.get("note"),
                "detail": "G1이 미판정이라 이 주장을 대조할 수 없다",
            })

    for reversal in reversals:
        push("silent_reversal", {k: v for k, v in reversal.items() if k != "iteration"})

    critique = payload.get("critique") or {}
    for item in (critique.get("contract_review") or []):
        if isinstance(item, dict) and item.get("unjustified_proposal"):
            push("unjustified_proposal", {
                "id": item.get("id"),
                "pointer": item.get("pointer"),
                "finding": item.get("finding"),
                "detail": "근거 없는 계약 발의다. Refine에 넘기지 않고 여기에만 남는다",
            })

    scores = ((payload.get("eval") or {}).get("rubric_scores") or {}).get("scores") or {}
    weaknesses = [w for w in (critique.get("weaknesses") or []) if isinstance(w, dict)]
    for weakness in weaknesses:
        axis = str(weakness.get("axis") or "")
        score = scores.get(axis)
        if weakness.get("severity") == "high" and isinstance(score, (int, float)) and score >= AXIS_HIGH_SCORE:
            # 어긋남의 방향을 적는다. 누가 더 엄한지가 읽는 사람이 필요한 값이다. 게이트까지 셋이 갈린 자리는
            # 과제가 요구하는 "자기 평가와 계약 테스트 결과가 어긋난 지점"의 후보이므로 따로 표시한다.
            gate_verdict = "REJECT" if (gate.get("violations") or []) else "PASS"
            three_way = gate_verdict == "PASS"
            direction = "Critique가 더 엄하다"
            note = (
                f"게이트는 위반을 찾지 못했고 Eval은 {score}를 줬는데 Critique만 high로 지적했다. 셋이 갈렸다."
                if three_way else
                f"게이트도 위반을 찾았으니 갈린 것은 Eval과 Critique다. Eval이 {score}로 통과시킨 축이다."
            )
            push("axis_disagreement", {
                "axis": axis,
                "critique_id": weakness.get("id"),
                "issue": weakness.get("issue"),
                "eval_score": score,
                "gate_verdict": gate_verdict,
                "direction": direction,
                "three_way": three_way,
                "detail": note,
            })

    # 게이트가 세는 항목을 Critique가 다시 말한 자리.
    #
    # 겹친다고 말하려면 같은 좌표를 가리켜야 한다. 축 이름이나 iteration이 같은 것은 겹침의 근거가 아니고,
    # 이름 하나가 문장에 나오는 것도 아니다. 관찰 규칙이 한 종류뿐인 run에서 그 느슨한 기준이 약점 다섯 개를
    # 그 하나에 전부 짝지었고, 그러면 리포트가 진짜 비평을 두고 "프롬프트를 고쳐라"라고 말한다. 이 절의 목적은
    # 기계가 이미 세는 것을 비평이 다시 말하는 낭비를 찾는 것인데 그 반대로 작동한 것이다.
    gate_places: list[tuple[tuple[str, ...], str, str]] = []
    for finding in list(gate.get("violations") or []) + list(gate.get("observations") or []):
        if not isinstance(finding, dict):
            continue
        for place in finding_places(finding):
            gate_places.append((place, str(finding.get("rule") or ""), str(finding.get("where") or "")))

    for weakness in weaknesses:
        target = weakness.get("target") if isinstance(weakness.get("target"), dict) else {}
        places = coordinate_places(" ".join([
            str(weakness.get("issue") or ""),
            str(weakness.get("suggestion") or ""),
            str((target or {}).get("pointer") or ""),
        ]))
        hit = next(((rule, where, place) for place, rule, where in gate_places if place in places), None)
        if hit:
            rule, where, place = hit
            push("machine_checkable_restatement", {
                "critique_id": weakness.get("id"),
                "axis": weakness.get("axis"),
                "rule": rule,
                "where": where,
                "shared": ".".join(place),
                "detail": f"게이트가 {rule}로 이미 세는 좌표 `{'.'.join(place)}`를 비평이 다시 말했다",
            })
