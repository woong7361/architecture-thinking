# feature-review 뼈대 명세

> 이 문서는 [design.md](design.md)를 구현할 때 모듈 경계와 인터페이스를 고정한다. 여러 작업자가 병렬로 만들어도 맞물리도록, 파일마다 맡는 일과 공개 함수의 시그니처를 여기서 정한다. 시그니처를 바꿔야 하면 이 문서를 먼저 고친다.

## 1. 원칙

파일 하나는 한 가지 일만 한다. runner는 단계를 어떤 순서로 부를지만 알고, git 호출, 프롬프트 조립, 파일 입출력, 검사 규칙, 리포트 서식은 모르고 각 모듈의 함수를 호출만 한다. 한 파일이 두 번째 일을 맡기 시작하면 파일을 나눈다.

- 설치 위치: `~/.agents/skills/feature-review/`. `~/.claude/skills/feature-review`는 이 디렉터리를 가리키는 디렉터리 연결이다.
- 언어: Python 3.12, 표준 라이브러리와 `jsonschema`만 쓴다. 테스트는 `unittest`로 쓴다. pytest는 설치되어 있지 않다.
- 모든 파일은 UTF-8로 읽고 쓴다.
- 경로 계산은 `pipeline/paths.py` 한 곳에서 한다. 다른 모듈은 skill 루트 경로를 스스로 계산하지 않는다.
- import는 패키지 형식으로 쓴다. 예: `from pipeline.clients.base import StageCall`. `pipeline/`과 그 하위 디렉터리마다 `__init__.py`를 둔다.
- 실행은 `python "<skill 루트>/pipeline/runner.py" [인자]`이고, 작업 디렉터리는 리뷰할 워크스페이스다. runner.py 맨 위에서만 skill 루트를 `sys.path`에 넣는 두 줄짜리 부트스트랩을 허용한다.
- 테스트는 skill 루트에서 `python -m unittest discover -s tests -t .`로 돌린다.

## 2. 디렉터리와 책임

```
~/.agents/skills/feature-review/
  SKILL.md                    intake 절차와 runner 호출 방법
  config.toml                 provider별 모델, 추론 강도, 실행 파일, 제한 시간
  prompts/                    단계별 프롬프트 본문. 코드 없음
  schemas/                    단계별 입출력 JSON 스키마. 코드 없음
  pipeline/
    runner.py                 명령행 인자를 받아 단계 순서와 병렬 실행만 조율
    paths.py                  skill 루트, prompts, schemas, runs, config 경로
    inputs.py                 IntakeInputs 데이터 정의
    settings.py               config.toml을 읽어 ProviderSettings 생성
    workspace.py              작업 디렉터리에서 git 최상위와 워크스페이스 식별자 계산
    run_store.py              실행 기록 디렉터리 생성과 산출물 읽기·쓰기
    progress.py               콘솔 진행 표시
    collect.py                git 정보와 사용자 입력 파일 수집. LLM 없음
    git_cmd.py                git 명령 실행과 출력 분해
    work_units.py             최근 기록에서 작업 단위 후보 생성
    decisions.py              이전 결정 파일 파싱
    prompt_builder.py         프롬프트 파일을 읽고 입력 블록을 붙여 최종 프롬프트 생성
    stage_inputs.py           단계마다 수집 결과에서 필요한 부분만 골라 입력을 만듦
    redact.py                 프롬프트 입력에서 skill 디렉터리 경로를 가림
    lens_review.py            관점 하나의 연쇄: 관점 → 결정적 검사 → 검증 → 판정 적용, 실패 기록
    stage_step.py             단계 호출 하나를 진행 표시로 감쌈
    clients/
      base.py                 AccessMode, StageCall, LLMClient 인터페이스
      factory.py              ProviderSettings로 클라이언트 생성
      claude.py               Claude CLI 호출
      codex.py                Codex CLI 호출
      json_output.py          CLI 출력에서 JSON 추출
      process.py              CLI 프로세스 실행과 제한 시간 초과 시 프로세스 트리 종료
      fake.py                 테스트용 가짜 클라이언트
    stages/
      call.py                 단계 공통 호출 절차: 프롬프트 조립, 저장, 클라이언트 호출
      scope.py                기능 범위 확정 단계
      criteria.py             판정 기준 확정 단계
      lens.py                 관점 단계. 관점 이름을 인자로 받음
      verify.py               LLM 검증 단계. 관점 이름을 인자로 받음
    checks/
      schema.py               JSON 스키마 검사
      citation.py             인용 위치와 코드 조각 검사
      required_fields.py      분류별 필수 필드 검사
    validate.py               checks를 조합해 항목을 통과와 기각으로 나눔
    verdicts.py               검증 판정을 항목에 적용
    behavior_conversion.py    문제가 아닌 기획 항목을 동작 명세 문장으로 옮김
    links.py                  사라진 항목을 가리키는 관련 항목 연결 정리
    consistency.py            병합 뒤 결과 항목, 동작 명세, 요구사항 추적의 정합성 맞춤
    merge.py                  관점별 결과 병합과 중복 제거
    report.py                 리포트 데이터 조립
    render.py                 리포트 데이터를 markdown으로 변환
  tests/                      unittest. 모듈마다 test_<모듈>.py
  runs/                       실행 기록. 코드가 만든다
```

## 3. 인터페이스

아래 시그니처는 공개 함수만 적는다. 내부 함수는 각 파일이 자유롭게 둔다.

### 3.1 기반

```python
# paths.py
SKILL_ROOT: Path          # 이 파일 기준 상위 두 단계
PROMPTS_DIR: Path
SCHEMAS_DIR: Path
RUNS_DIR: Path
CONFIG_PATH: Path

# inputs.py
@dataclass(frozen=True)
class IntakeInputs:
    feature: str | None             # 사용자가 자연어로 말한 리뷰할 기능
    spec_paths: tuple[Path, ...]    # 기획서 경로. 없으면 빈 튜플
    decisions_path: Path | None     # 이전 결정 파일
    provider: str | None            # None이면 config의 기본 provider

# settings.py
@dataclass(frozen=True)
class ProviderSettings:
    provider: str                   # "claude" | "codex"
    model: str
    effort: str                     # "high" 등
    bin: str                        # 실행 파일 이름
    timeout_seconds: int

def load_settings(config_path: Path, provider_override: str | None) -> ProviderSettings: ...

# workspace.py
@dataclass(frozen=True)
class Workspace:
    root: Path                      # git 최상위. git이 아니면 호출 위치
    invoked_from: Path              # 호출 위치
    is_git: bool
    workspace_id: str               # "<이름>-<root 경로 해시 8자리>"

def resolve_workspace(cwd: Path) -> Workspace: ...

# run_store.py
class RunStore:
    def __init__(self, runs_dir: Path, workspace: Workspace, started_at: datetime): ...
    @property
    def run_dir(self) -> Path: ...  # runs/<workspace_id>/<YYYYMMDD-HHMMSS>/
    def path(self, name: str) -> Path: ...          # run_dir 아래 상대 경로. 상위 디렉터리 생성
    def write_json(self, name: str, data: dict) -> Path: ...
    def read_json(self, name: str) -> dict: ...
    def write_text(self, name: str, text: str) -> Path: ...

# progress.py
class Progress:
    def start(self, stage: str) -> None: ...
    def done(self, stage: str, summary: str = "") -> None: ...
    def fail(self, stage: str, error: str) -> None: ...
```

`config.toml`의 형식은 다음과 같다.

```toml
default_provider = "claude"
timeout_seconds = 900

[providers.claude]
bin = "claude"
model = "claude-opus-5"
effort = "high"

[providers.codex]
bin = "codex"
model = "gpt-5.6-luna"
effort = "high"
```

### 3.2 LLM 클라이언트

```python
# clients/base.py
class AccessMode(Enum):
    ISOLATED = "isolated"           # 파일 도구 없음, 빈 임시 디렉터리
    REPO_READ = "repo-read"         # 읽기와 검색만, workdir에서 실행

@dataclass(frozen=True)
class StageCall:
    stage: str                      # 예: "scope", "lens.spec", "verify.error"
    prompt: str                     # 최종 프롬프트 전체
    output_schema: Path
    output_path: Path               # 파싱한 JSON을 여기에 쓴다
    mode: AccessMode
    workdir: Path | None            # REPO_READ일 때 필수

class LLMClient(ABC):
    @abstractmethod
    def run(self, call: StageCall) -> dict: ...   # 결과 JSON을 output_path에 쓰고 반환

# clients/factory.py
def create_client(settings: ProviderSettings) -> LLMClient: ...

# clients/json_output.py
def extract_json(text: str) -> dict: ...          # 코드 블록 감싸기 제거 후 파싱. 실패 시 ValueError

# clients/fake.py
class FakeClient(LLMClient):
    def __init__(self, responses: dict[str, dict]): ...   # stage 이름 → 돌려줄 JSON
    calls: list[StageCall]                                # 받은 호출 기록
```

프롬프트에는 실행 기록 디렉터리나 skill 디렉터리의 경로를 넣지 않는다. design.md 12절의 정보 차단 때문이다.

### 3.3 수집과 입력 파일

```python
# collect.py
def collect(workspace: Workspace, inputs: IntakeInputs) -> dict: ...
# schemas/collect.schema.json 형식. git 정보, 커밋하지 않은 변경분,
# 작업 단위 후보(git.work_units), 기획서 내용, 이전 결정 내용

# git_cmd.py
def run_git(root: Path, *args: str) -> str | None: ...
def lines(output: str | None) -> list[str]: ...
def ref_exists(root: Path, ref: str) -> bool: ...

# work_units.py
def merged_branch(subject: str) -> str | None: ...
def work_unit_candidates(root: Path, uncommitted_files: list[str]) -> dict: ...
# {"candidates": [{id, kind: uncommitted|merge|commit, head, branch, commits, files, ...}], "truncated": bool}

# decisions.py
def parse_decisions(path: Path) -> list[dict]: ...
# 이전 리포트의 확인사항 답과 동작 명세 표시를 읽는다.
# 각 항목: {"id", "kind": "question"|"behavior", "statement", "answer": "match"|"differs"|"chosen"|None, "note"}
```

### 3.4 LLM 단계

모든 단계 모듈은 `run` 함수 하나를 공개한다. 단계 모듈은 프롬프트 조립을 `prompt_builder`에, 호출을 `LLMClient`에, 저장을 `RunStore`에 맡기고, 자신은 어떤 입력을 어떤 프롬프트와 스키마로 보낼지만 안다.

```python
# prompt_builder.py
def build_prompt(prompt_name: str, blocks: dict[str, object]) -> str: ...
# prompts/<prompt_name>.md 본문 뒤에 blocks를 "## <키>" 제목과 JSON 코드 블록으로 붙인다.

# stages/scope.py
def run(client: LLMClient, store: RunStore, workspace: Workspace, collected: dict) -> dict: ...
# stages/criteria.py
def run(client: LLMClient, store: RunStore, workspace: Workspace, collected: dict, scope: dict) -> dict: ...
# stages/lens.py
LENSES = ("spec", "error", "security", "design")
def run(client: LLMClient, store: RunStore, workspace: Workspace, lens: str,
        scope: dict, criteria: dict) -> dict: ...
# 반환: {"findings": [...], "behavior_spec": [...], "requirement_trace": [...]}.
# behavior_spec과 requirement_trace는 spec 관점만 채운다.
# stages/verify.py
def run(client: LLMClient, store: RunStore, workspace: Workspace, lens: str, criteria: dict,
        findings: list[dict], behavior_spec: list[dict]) -> dict: ...
# 반환: {"verdicts": [...]}. 검증자에게는 항목의 주장, 위치, 인용, 분류별 필수 필드만 넘긴다.
# criteria는 기획 관점 검증자에게만 넘긴다. "요구사항으로 이미 정해진 결정인가"를 판정하려면 필요하다.
```

프롬프트 파일 이름은 `scope`, `criteria`, `lens_spec`, `lens_error`, `lens_security`, `lens_design`, `verify_spec`, `verify_error`, `verify_security`, `verify_design`이다.

### 3.5 결정적 처리

```python
# checks/schema.py
def check(item: dict, schema: dict) -> list[str]: ...              # 위반 사유 목록. 없으면 빈 목록
# checks/citation.py
def check(item: dict, workspace_root: Path) -> list[str]: ...      # 파일 존재, 줄 범위, 인용 일치. 공백 차이 무시
# checks/required_fields.py
def check(item: dict) -> list[str]: ...                            # design.md 8.1의 분류별 필수 필드

# validate.py
@dataclass(frozen=True)
class ValidationResult:
    passed: list[dict]
    rejected: list[dict]            # {"item": ..., "reasons": [...]}
def validate_findings(findings: list[dict], workspace_root: Path) -> ValidationResult: ...
def validate_behavior(statements: list[dict], workspace_root: Path) -> ValidationResult: ...

# verdicts.py
def apply_verdicts(items: list[dict], verdicts: list[dict]) -> tuple[list[dict], list[dict]]: ...
# (유지된 항목, 기각된 항목). revise 판정은 바뀐 필드를 반영한 항목으로 유지한다.
# 판정이 없는 항목은 유지하되 "unverified" 표시를 붙인다.

# behavior_conversion.py
def split_converted(findings: list[dict]) -> tuple[list[dict], list[dict]]: ...
# (남은 결과 항목, 동작 명세로 옮긴 문장). lens_review가 apply_verdicts 뒤에 부른다.

# links.py
def resolve_related(findings: list[dict], behavior_spec: list[dict], aliases: dict | None = None) -> tuple[list[dict], list[dict]]: ...
def merged_aliases(findings: list[dict]) -> dict[str, str]: ...

# consistency.py
def reconcile(findings, behavior_spec, requirement_trace) -> ConsistencyResult: ...
# ConsistencyResult(findings, behavior_spec, requirement_trace, record). record는 outputs/consistency.json에 남는다.
# 합쳐진 항목을 가리키는 연결은 남은 항목으로 바꾸고, 기각되거나 없는 항목을 가리키는 연결은 지운다.

# merge.py
def merge(per_lens: dict[str, list[dict]]) -> list[dict]: ...
# design.md 9절 규칙. 위치가 겹치고 같은 원인이면 하나로, 에러와 보안이 겹치면 보안으로.

# report.py
def assemble(scope: dict, criteria: dict, findings: list[dict], behavior_spec: list[dict],
             requirement_trace: list[dict], rejected: dict[str, list[dict]]) -> dict: ...
# schemas/report.schema.json 형식

# render.py
def render(report: dict) -> str: ...     # design.md 10절의 섹션 순서와 항목 서식
```

### 3.5.1 관점 연쇄와 단계 감싸기

```python
# lens_review.py
class LensResult(NamedTuple):
    findings: list[dict]
    behavior_spec: list[dict]
    requirement_trace: list[dict]
    rejected: list[dict]
def review(client, store, workspace, progress, lens_name: str, scope: dict, criteria: dict) -> LensResult: ...
# 실패하면 빈 결과와 {"item": None, "reasons": ["lens_failed: <오류>"]} 한 건을 돌려준다.

# stage_step.py
def run_step(progress: Progress, stage: str, fn, *args): ...
```

### 3.6 runner

```python
# runner.py
def main(argv: list[str] | None = None) -> int: ...
```

runner가 하는 일은 다음 순서의 호출뿐이다.

1. 인자를 `IntakeInputs`로 만든다. 인자는 `--feature`, `--spec`(여러 번), `--decisions`, `--provider`.
2. `load_settings`, `resolve_workspace`, `RunStore`, `create_client`를 만든다.
3. `collect` → `scope.run` → `criteria.run`.
4. 관점 네 개를 병렬로 실행한다. 관점마다 `lens_review.review`를 부르고, 그 안에서 `lens.run` → `validate_findings`, `validate_behavior` → `verify.run` → `apply_verdicts`를 이어서 한다.
5. `merge` → `links.resolve_related` → `consistency.reconcile` → `report.assemble` → `render` → 리포트 저장 → 리포트 경로를 표준 출력에 한 줄로 쓴다.

runner 안에 git 호출, 문자열 서식, 검사 규칙, JSON 필드 조작을 넣지 않는다.

## 4. 스키마

`schemas/`의 파일은 다음과 같다. 필드는 design.md 6절, 7.1절, 13절을 따른다.

- `collect.schema.json`: 수집 결과
- `scope.schema.json`: 찾은 기능, 그 기능을 추가한 변경, 판단 근거, 확신도, 리뷰 대상 파일
- `criteria.schema.json`: 요구사항 문장 목록. 문장마다 출처와 강도
- `finding.schema.json`: 결과 항목 하나
- `behavior.schema.json`: 동작 명세 문장 하나
- `lens_output.schema.json`: 관점 단계 출력
- `verify_output.schema.json`: 검증 단계 출력
- `report.schema.json`: 조립된 리포트 데이터

LLM이 채우는 스키마는 Codex의 `--output-schema`가 받아들이는 엄격한 형식으로 쓴다. 모든 객체에 `additionalProperties: false`를 두고 모든 속성을 `required`에 넣으며, 값이 없을 수 있는 속성은 null을 허용하는 타입으로 둔다.

## 5. 아직 정하지 않은 것

- 추가된 기능의 시작 지점은 design.md 15절의 열린 결정이다. 뼈대에서는 scope 단계의 LLM이 기능 설명과 커밋 메시지로 판단하게 두고, 판단 근거와 확신도를 결과에 남긴다.
- 리뷰 범위는 실행 한 번에 기능 하나다. 변경분에 무관한 기능이 여럿 섞여 있으면 scope 단계가 하나를 고르고 나머지는 이번 실행에서 제외했다고 기록한다. design.md 6.2절은 기능마다 따로 리뷰하라고 했으므로, 여러 기능을 한 번에 도는 것은 이후 과제다.
- 프롬프트 본문은 뼈대 단계에서 각 단계의 역할, 입력, 출력, 금지 사항까지만 쓴다. 문구 다듬기는 실제 실행 결과를 보며 한다.
