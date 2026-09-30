# 빌드 순서와 검증 방법

설계는 [docs/v0-design.md](docs/v0-design.md)다. 조각마다 소유 경로가 다르고, 각 조각은 혼자 돌려서 확인할 수
있어야 한다. 전체 run은 조각이 다 붙은 뒤 한 번만 돌린다.

## 조각

| 조각 | 소유 경로 | 혼자 확인하는 방법 |
| --- | --- | --- |
| A 스켈레톤 | `phase2/taskE/task5/skeleton/` | `mvnw test` 로 ArchUnit 통과, 부팅 후 `/v3/api-docs` 덤프 |
| B 파이프라인 골격 | `.codex/skills/contract-impl-agent/pipeline/{runner,validate,run_draft,run_stage,intake_to_input}.py`, `pipeline/stages/`, `pipeline/schemas/` | 샘플 input 검증, 단계별 `run_stage.py --stage` 진입 |
| C 결정적 장치 | `.codex/skills/contract-impl-agent/{rules/,pipeline/gates/,pipeline/tools/,tests/}` | 실제 계약에 추출기 돌리기, 픽스처로 게이트 판정 확인 |
| D 프롬프트와 루브릭 | `pipeline/prompts/`, `pipeline/rubrics/` | 스키마·축 집합 대조 |
| E 원장과 리포트 | `pipeline/ledger.py`, `pipeline/report.py` | 두 iteration 픽스처로 원장 갱신 확인 |

A와 B와 C는 서로 의존하지 않으므로 함께 만든다. D와 E는 C의 산출 형태가 실물로 나온 뒤에 붙인다.

## 게이트 인터페이스

B와 C가 같은 자리에서 만나므로 시그니처를 먼저 고정한다. C가 구현하고 B가 호출한다.

```python
# pipeline/gates/__init__.py
def run_g0(draft_files_dir: Path, skeleton_dir: Path, work_dir: Path) -> dict
    # {"status": "PASS"|"REJECT", "violations": [...], "derived_spec": <path or None>, "log": str}

def run_g1(derived_spec: dict, contract: dict, scope: list[str],
           decisions: list[dict], rules: dict) -> dict
    # {"violations": [...], "observations": [...]}

def check_contract_changes(prev_contract: dict, curr_contract: dict,
                           changes: list[dict], prev_path: str, curr_path: str) -> dict
    # {"violations": [...]}
```

위반과 기록은 같은 모양이다.

```python
{"rule": "response.field_missing", "point": "createAlert:201",
 "where": "Alert.lastCheckedAt", "detail": "...", "verdict": "violation"|"observation"}
```

## 패키지 규약

스켈레톤과 생성 코드가 같은 규약을 쓴다. ArchUnit 규칙이 이것을 강제하고, 입력의 `layout_rules`가 같은 말을 한다.

- `com.thinking.tennis.port` — 포트 인터페이스. 사람이 소유한다
- `com.thinking.tennis.adapter` — 아웃바운드 어댑터. 스켈레톤의 인메모리 스텁이 여기 있다
- `com.thinking.tennis.api` — 인바운드 어댑터. 컨트롤러와 DTO와 에러 매핑
- `com.thinking.tennis.app` — 유스케이스
- `com.thinking.tennis.domain` — 도메인

ArchUnit 규칙 셋이다. `domain`과 `app`은 HTTP·직렬화 타입을 참조하지 않는다. `api`는 `adapter`를 참조하지 않는다.
`domain`은 `api`를 참조하지 않는다.

## 외부 의존과 저장

외부 예약처는 실제로 부르지 않는다. 스켈레톤이 `CourtAvailabilityPort`와 그 인메모리 구현을 제공하고,
생성 코드는 포트만 호출한다. 저장도 인메모리다. MySQL과 Testcontainers는 v0에서 쓰지 않는다.
부팅이 빨라야 게이트가 iteration마다 돌 수 있고, 계약 적합성 판정에 실제 저장소가 필요하지 않다.

## 단계별 확인에서 전체 실행까지

1. A·B·C를 각각 혼자 확인한다.
2. D와 E를 붙이고 스키마와 픽스처로 확인한다.
3. `run_stage.py --stage gen` 으로 LLM 한 번만 호출해 매니페스트가 스키마를 통과하는지 본다.
4. 그 draft로 `--stage gate` 를 돌려 위반 목록이 나오는지 본다.
5. `--stage critique`, `--stage eval` 을 각각 한 번씩 돌린다.
6. 마지막에 `run_draft.py` 로 전체를 한 번 돌려 `max_iterations`까지 가는지, API 구현 초안과 리포트가
   나오는지 본다.
