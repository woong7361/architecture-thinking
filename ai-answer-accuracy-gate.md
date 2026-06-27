# AI 답변 정확성 게이트 논점 정리

## 우리가 고민하는 문제

현재의 핵심 문제는 AI의 답변 품질, 특히 **정확성**을 사용자의 직감, 컨디션, 그날의 추궁 능력에 맡기지 않고 반복 가능한 시스템으로 만들 수 있는가이다.

T7에서 AI를 날카롭게 추궁하면서 좋은 답변을 끌어냈지만, 그 방식은 사람의 대화 능력에 크게 의존한다. 실무에서 재현 가능한 품질을 얻으려면 "잘 물어보는 사람의 직감"을 시스템 안의 검증 장치로 바꿔야 한다.

따라서 질문은 다음과 같다.

```text
AI가 답변을 잘했는지 사람이 매번 감으로 판단하지 않고,
답변 생성 후 정확성 벤치마크를 돌려
통과한 답변만 사용자에게 보여줄 수 있는가?
```

## 핵심 쟁점

### 1. 정확성은 직접 판정하기 어렵다

"이 답변이 정확한가?"는 너무 큰 질문이다. 이 질문 자체는 비결정적이다. 답변의 품질, 맥락 적합성, 설명의 깊이, 누락 여부를 한 번에 결정적으로 판별하기 어렵기 때문이다.

실무적으로는 답변 전체가 아니라 **답변 안의 claim**을 기준으로 봐야 한다.

```text
답변 정확성 =
- 답변의 주요 claim이 근거를 가지는가
- 근거와 모순되지 않는가
- 제공된 문맥 밖의 내용을 사실처럼 말하지 않는가
- 추정은 추정으로 표시되는가
- 사용자 질문의 핵심을 누락하지 않는가
```

즉, 정확성 벤치마크의 단위는 `answer`가 아니라 `claim`이다.

### 2. 결정적 검증과 비결정적 평가를 분리해야 한다

모든 검증을 결정적으로 만들 수는 없다. 하지만 일부는 확실히 결정적으로 만들 수 있다.

결정적으로 검증 가능한 것:

- 파일이나 라인 참조가 실제 존재하는가
- 명령 실행 결과와 답변의 주장이 일치하는가
- 테스트, 빌드, 린트가 통과했는가
- JSON schema가 valid인가
- 필수 필드가 채워졌는가
- 금지 패턴이 포함되지 않았는가

비결정적 평가가 필요한 것:

- 이 claim이 정말 근거 없는 주장인가
- 사용자 질문의 핵심 요구를 누락했는가
- 답변이 문맥을 왜곡했는가
- 설계 trade-off를 충분히 다뤘는가

따라서 구조는 다음과 같이 나뉘어야 한다.

```text
결정적 검증 = script, schema, test, hook의 pass/fail
비결정적 평가 = evaluator AI의 claim grounding, coverage 평가
최종 차단 = hook 또는 runner의 hard gate
```

중요한 점은 evaluator AI의 판단은 여전히 비결정적이라는 것이다. 다만 hook은 evaluator가 낸 구조화된 결과를 기준으로 결정적인 pass/fail을 수행할 수 있다.

### 3. AGENTS.md만으로는 부족하다

`AGENTS.md`는 기준을 선언할 수 있지만, 기준을 강제하지는 못한다.

`AGENTS.md`가 할 수 있는 일:

- 정확성 원칙을 선언한다.
- 답변 시 지켜야 할 행동 규칙을 정의한다.
- 실패 시 재작성하거나 확인 요청하라는 정책을 둔다.
- 반복되는 실패를 skill, hook, script로 승격하라는 기준을 둔다.

`AGENTS.md`가 하기 어려운 일:

- 근거 없는 claim을 자동으로 차단한다.
- 질문 누락을 자동으로 감지한다.
- 답변 점수를 계산한다.
- 기준 미달 답변을 실제로 내보내지 못하게 막는다.

따라서 `AGENTS.md`는 헌법처럼 기준을 담고, 실제 집행은 hook, script, evaluator가 맡아야 한다.

### 4. hook은 실행과 차단을 맡아야 한다

hook의 역할은 "좋은 답변을 하라"고 지시하는 것이 아니라, 답변 생성 이후 검증 파이프라인을 실행하고 통과하지 못한 답변을 막는 것이다.

예상 흐름:

```text
user question
  -> draft answer
  -> claim extraction
  -> grounding / coverage evaluation
  -> deterministic gate script
  -> pass면 최종 응답
  -> fail이면 revise loop
  -> max loop 초과 시 중단 또는 사용자 확인 요청
```

hook이 직접 판단하지 않아도 된다. hook은 evaluator와 script를 호출하고, 그 결과가 기준을 만족하는지만 판정한다.

### 5. 벤치마크가 있어야 loop가 의미를 가진다

벤치마크 없이 loop를 돌리면 AI가 자기 답변을 다시 보고 "좋아진 것 같다"고 말하는 수준에 머무른다. 이는 여전히 비결정적이다.

벤치마크가 있으면 loop는 다음처럼 작동한다.

```text
초안 생성
  -> 벤치마크 검사
  -> 실패 항목 식별
  -> 실패 항목만 수정
  -> 재검사
  -> 통과 또는 중단
```

따라서 "loop를 돌린다"는 말은 반드시 "무엇을 기준으로 통과/실패를 판정하는가"와 함께 정의되어야 한다.

## 정확성 벤치마크 초안

### Hard fail

아래 항목은 하나라도 발생하면 점수와 무관하게 실패로 본다.

```yaml
hard_fail:
  - fabricated_file_or_line_reference
  - contradicted_by_source
  - unsupported_factual_claim
  - hidden_failed_command
  - stale_claim_without_freshness_check
  - answered_beyond_available_context_as_fact
```

### Scoring axes

점수화가 필요한 경우 다음 축으로 나눈다.

```yaml
scoring:
  grounding: 40
  coverage: 25
  contradiction_free: 20
  uncertainty_handling: 10
  concision_relevance: 5
```

각 축의 의미:

| 축 | 의미 |
| --- | --- |
| `grounding` | 주요 claim이 파일, 문서, 명령 결과, 공식 출처와 연결되는가 |
| `coverage` | 사용자 질문의 핵심 요구를 빠뜨리지 않았는가 |
| `contradiction_free` | 제공된 문맥이나 출처와 모순되지 않는가 |
| `uncertainty_handling` | 추정, 한계, 확인 필요를 사실처럼 말하지 않았는가 |
| `concision_relevance` | 정확성을 해치지 않으면서 질문 범위에 머무르는가 |

### Pass rule

초기 통과 기준은 다음처럼 잡을 수 있다.

```yaml
pass:
  hard_fail_count: 0
  total_score: ">= 85"
  grounding: ">= 35/40"
  contradiction_free: "20/20"
```

중요한 원칙은 **hard fail은 절대 게이트**라는 점이다. 예를 들어 총점이 92점이어도 `unsupported_factual_claim`이 하나 있으면 실패로 처리한다.

## 역할 분리

```text
AGENTS.md
  = 원칙, 기준, 실패 시 행동 정책

hook
  = 매번 검증 실행, pass/fail 차단, 로그 저장

evaluator AI
  = claim extraction, grounding 평가, question coverage 평가

script
  = schema 검증, 점수 계산, threshold 판정, loop 제어

benchmark dataset
  = 과거 실패 사례와 기대 답변을 모은 회귀 테스트 세트
```

이 구조에서 핵심은 "AI 평가를 믿는다"가 아니라 "AI 평가 결과를 구조화하고, 그 결과에 대한 통과 기준을 결정적으로 적용한다"이다.

## 실무 적용 방향

처음부터 큰 시스템을 만들 필요는 없다. 작은 벤치마크 세트로 시작해서 실제 실패를 누적하는 편이 낫다.

초기 MVP:

```text
1. 실제 대화 20개를 benchmark case로 저장한다.
2. 각 case마다 expected_points, forbidden_claims, required_uncertainty를 정의한다.
3. 답변 초안에서 claim을 추출한다.
4. claim별 evidence 여부와 질문 coverage를 evaluator가 평가한다.
5. script가 hard fail과 점수를 계산한다.
6. 기준 미달이면 최대 2회 revise loop를 돈다.
7. 그래도 실패하면 답변을 내보내지 않고 확인 필요로 중단한다.
```

벤치마크 케이스 예시:

```yaml
- id: agents-md-determinism-001
  user_input: "AGENTS.md만으로 결정적 검증이 가능해?"
  context_files:
    - AGENTS.md
  expected_points:
    - "AGENTS.md는 행동 규약이지 실행 검증 장치가 아니다"
    - "결정적 검증은 script/test/schema/hook에서 나온다"
    - "단일 대화에서는 self-check 정도만 가능하다"
  forbidden_claims:
    - "AGENTS.md만으로 완전한 결정적 검증이 가능하다"
    - "LLM self-check는 결정적이다"
  required_uncertainty:
    - "완전 결정성은 어렵다는 한계"
  pass_threshold: 85
```

운영 방식:

```text
AI가 틀린 답변을 한다
  -> 왜 틀렸는지 분류한다
  -> forbidden_claim 또는 expected_point를 추가한다
  -> benchmark dataset에 회귀 케이스로 넣는다
  -> 다음부터 같은 유형의 실패를 자동 검출한다
```

## 현재 잠정 결론

정확성 문제는 `AGENTS.md`만으로 해결하기 어렵다. `AGENTS.md`는 기준을 선언하는 곳이고, 정확성을 실제로 높이려면 hook, evaluator AI, deterministic script, benchmark dataset이 함께 필요하다.

가장 현실적인 구조는 다음과 같다.

```text
답변을 믿지 않는다.
답변이 벤치마크를 통과했는지를 믿는다.
```

그리고 벤치마크의 중심 질문은 "좋은 답변인가?"가 아니라 다음이어야 한다.

```text
근거 없는 주장, 출처와의 모순, 질문 누락, 불확실성 은폐가 0개인가?
```

이 기준을 통과하지 못한 답변은 재작성하거나, 그래도 통과하지 못하면 답변을 중단하고 사용자 확인을 요청해야 한다.

