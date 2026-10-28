# 빌드 순서와 검증 방법

설계는 [docs/v1-design.md](docs/v1-design.md)다. 조각마다 소유 경로가 다르고, 각 조각은 혼자 돌려서 확인할 수
있어야 한다. 전체 run은 조각이 다 붙은 뒤 한 번만 돌린다.

## 선행 작업

하네스 밖에서 먼저 끝나야 하는 것이 둘이다. 설계 §1의 단계 0과 단계 1이고, 둘의 산출이 하네스 입력이다.

| 작업 | 소유 경로 | 끝났다고 보는 조건 |
| --- | --- | --- |
| 단계 0 빨간불 고정 | `phase2/taskF/task4/step0/` | 실패 테스트 이름과 이유가 로그에 있고, 테스트 파일 해시가 기록됐다 |
| 단계 1 AI 구현 run | `phase2/taskF/task4/step1/` | 자기 평가, 구현 diff, 테스트 로그가 그 순서로 남았다 |

단계 1은 가림 목록을 먼저 확정한다. 설계 §4의 표가 무엇을 주고 무엇을 막는지 정하고, 가린 경로 목록을 run
폴더에 적어 둔다. 적어 두지 않으면 나중에 그 run이 무엇을 못 보고 만든 구현인지 재구성할 수 없다.

## 조각

| 조각 | 소유 경로 | 혼자 확인하는 방법 |
| --- | --- | --- |
| A 출처 스냅샷과 수집기 | `sources/`, `copies/`, `pipeline/tools/collect_source.py` | 알려진 문장 하나를 스냅샷에서 문자열로 찾고, 수집한 사본의 해시가 원장과 같은지 본다 |
| B 파이프라인 골격 | `pipeline/{runner,validate,run_stage}.py`, `pipeline/stages/`, `pipeline/schemas/` | 샘플 input 검증, 단계별 `run_stage.py --stage` 진입 |
| C 결정적 장치 | `rules/`, `pipeline/gates/`, `pipeline/tools/`, `tests/` | 지점 추출기를 실제 코드에 돌리고, 픽스처로 규칙별 판정을 확인 |
| D 프롬프트와 루브릭 | `pipeline/prompts/`, `pipeline/rubrics/` | 스키마와 축 집합 대조 |
| E 원장과 리포트 | `pipeline/ledger.py`, `pipeline/report.py` | 두 iteration 픽스처로 원장 갱신과 주장 집합 이력 확인 |

A와 B와 C는 서로 의존하지 않으므로 함께 만든다. D와 E는 C의 산출 형태가 실물로 나온 뒤에 붙인다.

## 게이트 인터페이스

B와 C가 같은 자리에서 만나므로 시그니처를 먼저 고정한다. C가 구현하고 B가 호출한다.

```python
# pipeline/gates/__init__.py

def extract_sites(code_roots: list[Path], rules: dict) -> list[dict]
    # [{"site": "path:line", "api": "<식별자>", "attrs": {...}}]
    # 판정 자산이다. 생성 쪽 payload에 넣지 않는다

def run_citation_gate(gen_output: dict, snapshot_root: Path,
                      copies_root: Path, grades: dict) -> dict
def run_coverage_gate(claims: list[dict], sites: list[dict]) -> dict
def run_coordinate_gate(claims: list[dict], repo_root: Path) -> dict
def compare_limiting_clauses(gen_output: dict, critique_output: dict) -> dict
def check_claim_changes(prev_claims: list[dict], curr_claims: list[dict],
                        reported: list[dict]) -> dict
```

위반과 기록은 같은 모양이다. `verdict`가 셋인 것이 `contract-impl-agent`와 다르다. 미판정을 위반과 다른
상태로 적어야 게이트 고장이 탈락으로 보이지 않는다.

```python
{"rule": "coverage_gap", "point": "<site 또는 claim-id>",
 "where": "<경로나 심볼>", "detail": "...",
 "verdict": "violation" | "unmeasured" | "observation"}
```

## 출처 스냅샷을 어떻게 만드는가

판본은 짐작하지 않고 해석된 값을 읽는다. 대상 코드의 `mvn dependency:list`가 실제로 쓰는 판본을 출력하고,
스냅샷 루트 이름에 그 판본을 적는다. 루트 이름과 실제 판본이 어긋나면 인용이 다른 판본을 가리킨다.

| 루트 | 받는 방법 |
| --- | --- |
| `sources/jdk-<ver>/` | `jar xf "$JAVA_HOME/lib/src.zip" java.base/java/util/concurrent/` |
| `sources/spring-framework-<ver>/` | `mvn dependency:copy -Dartifact=org.springframework:spring-tx:<ver>:jar:sources` 뒤 해제 |
| `sources/hibernate-orm-<ver>/` | `mvn dependency:copy -Dartifact=org.hibernate.orm:hibernate-core:<ver>:jar:sources` 뒤 해제 |
| `copies/` | Gen과 Critique가 수집 시점에 쓴다. 파일마다 원본 URL, 수집 시각, 해시를 원장에 남긴다 |

javadoc 스냅샷은 두지 않는다. JDK와 Spring의 javadoc은 소스 주석에 그대로 들어 있다.

`sources/`는 사람이 만들고 쓰기를 막는다. `copies/`는 수집 단계만 쓰고 그 뒤로는 읽기다. 둘 다 변경 목록에
나타나면 `judge_path_touched`로 탈락한다.

## 지점 추출기의 규칙은 데이터다

어떤 API를 지점으로 셀지는 `rules/concurrency_sites.yaml`에 둔다. 추출기 코드는 규칙을 읽어 돌 뿐이고,
다음 대상에서는 규칙만 갈아끼운다. 규칙 본문에 특정 클래스 이름을 박으면 그 대상에서만 도는 추출기가 된다.

규칙 하나는 식별 방법과 지점 단위를 적는다. 애노테이션은 선언 위치가 지점이고, 호출은 호출 위치가 지점이다.
한 선언에 속성이 여럿이면 속성마다 지점을 셀지 선언 하나로 셀지도 규칙이 정한다. 속성마다 세면 커버리지가
촘촘해지고 주장 수가 늘어난다.

## 리허설을 먼저 고정한다

게이트는 파이프라인을 붙이기 전에 양방향으로 고정한다. `tests/`에 손으로 쓴 정답 묶음 1건과, 규칙마다 그
규칙만 깨뜨린 입력 1건을 둔다. 정답이 통과하고 각 위반 입력이 그 규칙으로만 탈락하는지 자동 실행한다.

규칙별로 어떤 입력이 필요한지 적어 둔다. 입력이 없는 규칙은 공허한지 확인된 적이 없는 규칙이다.

| 규칙 | 그 규칙만 깨뜨리는 입력 |
| --- | --- |
| `claim_uncovered` | 인용 배열이 빈 주장 1건 |
| `claim_site_unknown` | 지점 목록에 없는 좌표를 단 주장 |
| `coverage_gap` | 지점 하나를 어느 주장도 덮지 않은 묶음 |
| `citation_absent` | 사본에 없는 경로, 그리고 경로는 맞고 문자열만 다른 인용 |
| `source_out_of_scope` | 블로그 URL을 사실 주장의 근거로 단 인용. 측정 로그를 사실 주장의 근거로 단 인용 |
| `anchor_absent` | `decides_at`의 심볼이 인용문에 없는 주장 |
| `metric_coordinate_absent` | 좌표 없는 교환 항목, 그리고 실존하지 않는 경로를 단 교환 항목 |
| `limiting_clause_missed` | Critique에만 있는 조항을 심은 한 쌍 |
| `claim_change_unreported` | 신고 없이 주장 하나를 지운 두 번째 iteration |
| `quote_oversized` | 인용 4개를 단 주장, 그리고 연속 41줄 인용 |
| `role_breach` | Gen 산출에 점수 필드를 넣은 입력 |
| `judge_path_touched` | 변경 목록에 `rules/` 경로를 넣은 입력 |

`source_out_of_scope`와 `metric_coordinate_absent`에 입력이 둘인 이유는 한 규칙이 두 가지를 보기 때문이다.
출처 등급은 경로의 등급과 쓰임의 짝을 보고, 좌표는 유무와 실존을 본다. 하나만 리허설하면 나머지 절반이
확인되지 않은 채 남는다.

## 단계별 확인에서 전체 실행까지

1. A·B·C를 각각 혼자 확인한다.
2. D와 E를 붙이고 스키마와 픽스처로 확인한다.
3. `run_stage.py --stage gen` 으로 LLM 한 번만 호출해 산출이 스키마를 통과하는지 본다.
4. 그 산출로 `--stage gate` 를 돌려 위반 목록이 나오는지 본다.
5. `--stage critique`, `--stage eval` 을 각각 한 번씩 돌린다.
6. 마지막에 전체를 한 번 돌려 반복 상한까지 가는지, 주장별 표와 리포트가 나오는지 본다.

## 복사한 파일

`contract-impl-agent`에서 그대로 가져온 것은 넷이다. **변경 금지다.** 고쳐야 할 것이 생기면 그 파일을
고치지 않고, 어느 조각이 왜 그 변경을 요구하는지 먼저 적는다.

| 파일 | sha256 앞 12자 |
| --- | --- |
| `pipeline/stages/scripts/llm_client.py` | `d78e1a665cd8` |
| `pipeline/stages/scripts/claude_client.py` | `0b4dba016385` |
| `pipeline/stages/scripts/codex_client.py` | `da20db053e24` |
| `pipeline/stages/scripts/textio.py` | `3e832243e589` |

## runner 는 복사하지 않았다

설계 §15는 runner 의 단계 오케스트레이션을 재사용으로 적었는데, 실물을 열어 보고 바꿨다.

`contract-impl-agent/pipeline/runner.py` 는 3,053줄이고 `contract` 가 129번 나온다. 계약 판본을 잠그고
올리는 일, 생성 코드를 git 에 커밋해 변경 목록을 뽑는 일, 스켈레톤을 복사하는 일이 그 안에 있다. 이
하네스는 읽기 전용이라 그 대상이 하나도 없다.

복사한 뒤 걷어내면 **복사한 파일을 고치지 않는다는 규약이 그 자리에서 깨진다.** 그러면 어느 쪽이
깨졌는지 가를 수 없게 되는데, 그것을 막으려고 둔 규약이다. 그래서 도메인 중립인 잎 파일 넷만 복사하고
오케스트레이션은 새로 썼다. 새로 쓴 쪽이 단계 그래프와 반복 상한과 원장이고, 가져온 구조는 단계마다
프롬프트 하나와 스키마 하나를 두는 형식, payload 를 라벨 붙은 JSON 블록으로 넘기는 형식, 위반을
`{rule, point, where, detail, verdict}` 로 적는 형식이다.

`payload.py` 도 복사하지 않고 새로 썼다. `block`·`compose`·`read_prompt` 는 모양을 그대로 가져왔고,
계약 판본 전용 함수는 대상이 없어 옮기지 않았다. 파일 머리에 그 사실을 적어 뒀다.

## 조각별 진행

| 조각 | 상태 | 확인한 방법 |
| --- | --- | --- |
| A 출처 스냅샷과 수집기 | 끝 | 루트 셋, 991개 파일. 결정 지점이 문자열로 있는지 확인. `--verify` 로 원장 대조 |
| B 파이프라인 골격 | 끝 | `runner.py --dry-run` 으로 단계별 payload 키 집합과 누출 검사 |
| C 결정적 장치 | 끝 | `tests/run_tests.py` 35건. 규칙마다 그 규칙만 깨뜨린 입력 |
| D 프롬프트와 루브릭 | 끝 | 리허설이 루브릭 축 집합과 스키마 enum 을 대조 |
| E 원장과 리포트 | 원장만 | 리포트 작성기가 아직 없다 |
