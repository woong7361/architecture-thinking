# Task F-4: AI 파이프라인 (JVM/Spring 소스코드 리딩 에이전트) + 명세를 판정 기준으로

(Grit's Why): 이것이 이번 Station의 진짜 산출물입니다. 동시성은 AI가 가장 자신 있게 틀리는 영역이라, "AI가 맞다고 한다"가 아니라 "소스와 재현 테스트가 맞다고 한다"로 판정 기준을 옮깁니다.

### 수행 내용

1. Phase 1/2-1 하네스를 재사용해 소스코드 리딩 에이전트로 확장하세요. 본인이 쓴 락 또는 격리 관련 코드의 근거를 JVM/Spring(또는 사용 라이브러리) 소스/공식 문서에서 찾게 하고(예: `ReentrantLock`의 공정성 옵션, `@Transactional`의 전파/격리 속성이 실제로 무엇을 거는지), 에이전트가 가져온 근거를 본인이 1차 출처와 대조해 채택/기각합니다. 어디를 재사용했고 무엇을 새로 만들었는지 적어 주세요.
2. **재현 먼저, 판정은 테스트로.** 이번 Station의 핵심 의식입니다.
  - F-2의 동시성 재현 테스트를 먼저 확정하세요(빨간불 고정).
  - 그다음 락 구현을 AI에게 맡기세요.
  - AI가 만든 구현을 본인의 동시성 재현 테스트로만 판정하세요. AI에게 "락 제대로 걸었어?"라고 묻지 말고, 재현 테스트의 통과/실패로 판정합니다. 테스트는 실패라는데 AI는 맞다고 우긴 지점이 있으면 그 지점과 본인 판단을 적어 주세요.

> 자가체크 안내: 위 파이프라인과 재현 테스트는 학습을 돕는 도구입니다. 통과 여부가 본인을 평가하는 시험이 아니라, "왜 깨지고 왜 막히는가"를 본인이 설명할 수 있게 만드는 학습 루프라는 점을 잊지 마세요.

### 제출물

- [ ] 소스코드 리딩 에이전트의 코드/프롬프트와, 에이전트가 가져온 근거를 1차 출처와 대조한 채택/기각 기록을 GitHub에.
- [ ] 재현 먼저 쓴 동시성 테스트와, AI 구현을 그 테스트로 판정한 결과. AI의 자기 평가와 재현 테스트 결과가 어긋난 지점 1개와 본인 판단. (최소 300자)

---

## 답안

### 제출물 1: 소스 리딩 에이전트와 채택·기각 기록

하네스는 [source-evidence-agent](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/source-evidence-agent)에 있다.
대상 코드가 기대는 전제를 주장으로 뽑고, 그 주장의 근거를 라이브러리 소스와 공식 문서에서 찾아와,
사람이 1차 출처와 대조해 채택·기각할 자료를 만든다.

| 제출물 | 자리 |
| --- | --- |
| 설계 | [v1-design.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/docs/v1-design.md) · [v0-design.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/docs/v0-design.md) |
| 파이프라인 코드 | [pipeline/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/source-evidence-agent/pipeline) |
| 프롬프트 5벌 | [prompts/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/source-evidence-agent/pipeline/prompts) |
| 스키마 5벌 | [schemas/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/source-evidence-agent/pipeline/schemas) |
| 루브릭 `claim:v1` | [claim.rubric.yaml](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/pipeline/rubrics/claim.rubric.yaml) |
| 결정적 게이트 | [gates/citation.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/pipeline/gates/citation.py) · [gates/sites.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/pipeline/gates/sites.py) |
| 규칙 카드 | [sources.yaml](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/rules/sources.yaml) · [concurrency_sites.yaml](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/rules/concurrency_sites.yaml) |
| 게이트 리허설 34건 | [tests/run_tests.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/source-evidence-agent/tests/run_tests.py) |
| run 기록과 리포트 | [runs/run3](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/source-evidence-agent/runs/run3) |
| **채택·기각 기록** | [step2/verdicts.md](https://github.com/woong7361/architecture-thinking/blob/main/phase2/taskF/task4/step2/verdicts.md) |
| 판정하면서 돌린 측정 | [step3/](https://github.com/woong7361/architecture-thinking/tree/main/phase2/taskF/task4/step3) |

---

### 제출물 2: 재현 먼저 쓴 테스트와 AI 구현의 판정

#### 무엇을 막으려 했나

같은 사용자가 같은 코트와 날짜와 시간대로 알림을 신청하면, 감시 중인 신청은 한 건만 남아야 한다.
지금 코드는 먼저 조회하고 없으면 만든다. 두 요청이 겹치면 둘 다 "없다"를 읽고 각각 만든다.

재현 테스트는 두 요청을 겹쳐서 100번 보낸다. 매번 DB 건수와 반환된 신청 번호를 확인한다. 고치기 전에는
100번 다 중복이 생겼다.

#### 어떤 순서로 했고 어떻게 나왔나

1. 재현 테스트를 빨간불로 고정했다
2. 조건을 주지 않고 AI에게 "동시 요청에서도 규칙이 지켜지게 해라"만 시켰다
3. AI가 만든 코드가 무엇을 전제하는지 하네스로 문장을 뽑고, Spring과 Hibernate 소스에서 근거를 찾게 했다
4. 그 문장 열 개를 하나씩 원문과 대조해 채택하거나 기각했다

AI 구현은 세 번 돌렸고 세 번 다 통과했다. 세 번 다 AI가 미리 적어 둔 예상 숫자가 실제 결과와 같았다.
자기 평가와 테스트 결과가 어긋난 지점은 없었다.

어긋난 곳은 하네스가 쓴 설명이었다. **열 개를 하나씩 열어 보니 채택 3, 기각 7이다.** 기각이 이렇게 많이
나올 거라고 예상하지 못했다.

#### 기각 일곱은 전부 코드만 봐서 생겼다

기각 사유를 모아 보니 원인이 하나였다. 설명이 소스를 잘못 읽은 게 아니라, **소스만 읽고 판단한 것**이
문제였다.

**동시성 코드의 전제는 대부분 다른 파일에 있는 값에 달려 있다.** `@Transactional`이 실제로 걸리는지는
Spring 설정이 정한다. 잠금문에 타임아웃이 붙는지는 DB 방언의 플래그가 정한다. `persist`가 INSERT를 바로
보내는지는 키 생성 전략이 정한다. 코드를 읽으면 `if` 문은 보이지만 그 `if`에 들어가는 값은 안 보인다.
기각된 설명 두 개가 똑같이 그랬다. `if`나 삼항 연산자를 읽고, 그 조건의 값이 어디서 오는지는 안 읽었다.

그래서 뒤집는 데도 소스가 아니라 실행 결과가 필요했다. **기각 일곱 중 다섯은 로그나 SQL을 보고 뒤집었고,
소스만 읽어서 뒤집은 건 한 건이다.**

#### 하네스는 의심 목록까지 만들고 거기서 멈췄다

하네스가 한 일 중 테스트가 못 하는 게 있었다. **무엇을 의심할지 목록을 만든 것이다.** 코드에서 동시성
관련 API가 쓰인 곳 24군데를 찾고, 거기서 "이 코드는 이걸 전제한다"는 설명 열 개를 뽑았다. 무엇을
의심해야 하는지는 테스트가 알려 주지 않는다. 있지도 않은 인용을 지어낸 것도 게이트가 사람보다 먼저 걸렀다.

설명마다 **반박을 따로 찾는 단계**도 돌았다. 그 설명을 깎을 만한 조항을 소스에서 찾아오게 한다. 처음
17건을 찾아왔고 그걸 반영하자 6건으로 줄었다. 기각 한 건은 그 반박이 근거였다.

완료가 된 후 판정에 대한 검토를 소스를 읽어서 했더니, 읽을 때마다 답이 왔다 갔다 했다. 같은 걸 실제로 돌려 로그를 보니 답이 한 번에 하나로 나왔다.

**그래서 반박을 글로 두지 말고 돌려 봐야 한다.** "이 조항이 맞으면 로그나 SQL에 무엇이 다르게 보일
것인가"를 적게 하고 한 번 돌리면 된다. 돌리는 쪽은 LLM이어도 되고 테스트여도 된다. 소스 인용이 아니라
실행 결과로 판정하면 된다.

#### 그런데 동시성은 돌려도 안 잡히지 않나

여기서 걸린다. 동시성은 같은 코드가 돌릴 때마다 다른 결과를 낸다. 한 번 통과한 것이 다음에도 통과한다는
보장이 없다. 그러면 돌려 보고 판정하자는 말이 무너진다.

**그런데 돌릴 때마다 달라지는 건 요청을 겹쳐 보냈을 때 나오는 결과다.** 신청이 몇 건 생기는지,
재시도가 몇 번 도는지가 그렇다. 설명이 묻는 건 대개 그게 아니었다. "여기서 트랜잭션이 열리는가",
"이 줄이 무슨 SQL을 내는가"였다. 이건 요청을 하나만 보내도 로그에 찍히고, 다시 돌려도 같게 찍힌다.

설명을 뒤집은 관측 넷이 다 그랬다.

| 관측 | 무엇을 봤나 |
| --- | --- |
| 트랜잭션 로그 | 트랜잭션이 열렸는지 |
| 실제로 나간 SQL | `WHERE`에 어떤 조건이 붙었는지 |
| 한 줄 지우고 비교 | 그 줄이 SQL을 바꾸는지 |
| 일회용 DB에 DDL만 올리기 | 제약이 어떤 입력을 막는지 |

넷 다 요청을 겹쳐 보내지 않았다. 경합이 끝난 뒤의 건수를 세는 게 아니라 로그에 찍힌 줄을 읽는다.
기각된 설명 일곱 중 여섯이 이렇게 뒤집혔다.

겹쳐 보낸 결과를 묻는 설명은 하나였다. "동시 요청이 셋 이상이면 재시도가 상한에 닿는다"는 설명이라
한 번 돌려서는 알 수 없다. 요청 수를 2·3·5·8로 올려 가며 30회씩 돌렸고, 여덟에서 240건 중 58건이
상한을 다 쓰고 실패했다. 이런 설명은 통과·실패가 아니라 비율로 받아야 한다.

**그러면 동시성도 둘로 나눠 잡을 수 있다. 요청을 겹쳐 보내는 테스트로 계약이 지켜지는지 넓게 잡고,
세부는 단위 테스트처럼 하나씩 잡는다.** 계약은 "감시 중인 신청은 한 건만 남는다"다. 세부는 "여기서
트랜잭션이 열린다", "이 잠금문에 타임아웃이 붙는다", "이 제약이 이 입력을 막는다" 같은 것들이다.

어려운 쪽은 세부다. 한 줄의 동작을 정하는 값이 Spring 설정 클래스와 DB 방언과 엔티티 매핑과 스키마에
나뉘어 있고, 마지막 답은 DB가 낸다. 자바 코드만 들고 단위 테스트처럼 끊어 잡기가 안 된다.

#### 그래서 어떻게 더 잘 잡을 것인가

테스트를 더 촘촘하게 쓰는 것만으로는 부족할 것 같다. 네 가지를 생각해 봤다.

**1. 설명과 반박 양쪽에 반증 관측을 적게 한다.** "이게 틀렸으면 로그나 SQL에 무엇이 다르게 보이는가"를
쓰게 한다. 읽기가 틀렸으면 예상도 틀릴 테니 한 번 돌려 보면 드러나지 않을까 싶다.

**2. 한 줄 지우고 돌려 비교한다.** "이 줄이 이걸 한다"는 설명에는 이게 제일 싸게 먹히는 반증 같다.
`flush()`를 빼고 돌렸더니 나가는 SQL이 26줄로 똑같았다. 그 줄은 아무 쓰기도 당기지 않았다.

**3. 숫자를 하나로 고정하지 않는다.** 동시 요청을 둘로만 재면 재시도 상한이 3이든 5든 결과가 같다.
값을 고를 근거가 되는 구간을 측정한 적이 없었다. 요청을 여덟으로 올리니 240건 중 58건이 상한을 다 쓰고
실패했다. 재시도 상한처럼 값을 골라야 하는 자리는 구간을 쓸어 봐야 할 것 같다.

**4. 실행되지 않은 경로를 센다.** AI는 방어를 두 겹으로 짰다. 사용자 행에 잠금을 걸어 요청을 줄 세우고,
그게 뚫리면 DB 유일 제약이 막게 했다. 두 번째 겹은 100번 중 한 번도 돌지 않았다. 중복 키 오류 로그가
0건이고 반환된 신청 번호가 빈칸 없이 100개 연속이라, 매번 INSERT가 한 번씩만 나갔다. 경로마다 카운터를
두면 설명이 말하는 경로가 실제로 돌았는지 알 수 있다. 돌지 않은 경로에 대한 설명은 통과가 아니라
미확인으로 남기는 게 맞다고 본다.
