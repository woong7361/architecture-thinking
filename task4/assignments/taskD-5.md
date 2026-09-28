# Task D-5: AI 파이프라인 (글쓰기 하네스 v1 + 스피치 리허설 에이전트)

(Grit's Why): 이것이 이번 Station의 진짜 산출물입니다. Phase 0 T8(또는 1-1~1-3)의 글쓰기 하네스 v0를 글쓰기·전달 도메인의 v1으로 고도화합니다. AI 파이프라인이 초안과 비평을 맡되, 저자 영역(메시지·논리)은 본인이 통제합니다. (하네스가 없다면 D-5 전에 v0부터 세우는 것까지가 범위입니다.)

### 수행 내용

1. 글쓰기 하네스 v1: 초안 생성(Gen) → 비평 에이전트(Critique, 새 세션에 '냉정한 시니어 리뷰어' 역할) → 퀄리티 평가(Eval, 루브릭 예: 메시지 명료성, 4-Step 충실도, 군더더기 없음, 독자 문제 정렬) → 퇴고. D-2/D-3 글을 이 파이프라인에 통과시키고, AI 제안 중 채택/기각을 본인이 판단한 기록을 남기세요.
2. 스피치 리허설 에이전트: D-4 녹화의 전사(transcript)를 입력하면 필러 워드(음/어/그) 빈도, 말 속도, 구조(도입-핵심-마무리)를 피드백하는 에이전트를 만드세요.
3. AI가 메시지나 논리를 대신 만들려 한 지점(저자 영역 침범)을 잡아낸 사례가 있으면 적으세요.

### 제출물

- [x] 글쓰기 하네스 v1 코드/프롬프트/루브릭 + 채택·기각 판단 로그.
- [x] 스피치 리허설 에이전트 코드 + D-4 녹화에 돌린 피드백 결과.
- [x] AI가 저자 영역(메시지·논리)을 넘보려 한 사례 + 본인 판단. (위반이 없었다면 AI를 초안·비평 영역에 머물게 한 본인 방식을 적으세요.) (최소 300자)

---

## 답안

### 1. 글쓰기 하네스 v1

- 스킬 전체: [.codex/skills/blog-draft/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/blog-draft)
- 파이프라인 오케스트레이션: [runner.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/blog-draft/pipeline/runner.py)
- 프롬프트: [pipeline/prompts/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/blog-draft/pipeline/prompts)
- 루브릭: [rubric.yaml](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/blog-draft/pipeline/rubric.yaml)
- 실행 및 변경 기록: [runs/reviewed/2026-08-20_2db504b3/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/blog-draft/runs/reviewed/2026-08-20_2db504b3)

---

### 2. 스피치 리허설 에이전트

#### 코드와 실행 구조

- 스킬 전체: [.codex/skills/speech-rehearsal/](https://github.com/woong7361/architecture-thinking/tree/main/.codex/skills/speech-rehearsal)
- 워크플로: [SKILL.md](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/SKILL.md)
- 검토 문맥 생성: [prepare_review.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/scripts/prepare_review.py)
- 결과 검증 및 집계: [aggregate_review.py](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/scripts/aggregate_review.py)
- 전달 검토 에이전트: [설정](https://github.com/woong7361/architecture-thinking/blob/main/.codex/agents/delivery-reviewer.toml), [역할 프롬프트](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/references/roles/delivery-reviewer.md), [루브릭](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/references/rubrics/delivery.yaml)
- 시니어 논리 검토 에이전트: [설정](https://github.com/woong7361/architecture-thinking/blob/main/.codex/agents/senior-logic-reviewer.toml), [역할 프롬프트](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/references/roles/senior-logic-reviewer.md), [루브릭](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/references/rubrics/logic.yaml)

#### D-4 녹음에 적용한 결과


| 대상          | 측정 결과                               | 핵심 피드백                                                                                                                                                                       | 실행 산출물                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------- | ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 3~5분 스피치    | 311.616초, 분당 85.87토큰, 확정 필러 6회      | 도입의 문제의식과 `테스트 개수보다 무엇을 막는지가 중요하다`는 중심 메시지, 행동 제안이 분명했다. 반면 긴 문장과 중복 연결어가 있었고 구간 속도가 분당 60~150토큰으로 흔들렸다. AI 검토와 테스트를 비교한 조건, 테스트 규칙을 검증하는 주체와 절차, 코드 변경 실험의 판정 기준은 보강이 필요했다. | [전체 피드백](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-explanation-v2-2026-08-30-1/feedback.md) · [전달 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-explanation-v2-2026-08-30-1/delivery-review.json) · [논리 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-explanation-v2-2026-08-30-1/logic-review.json) |
| 1분 엘리베이터 피치 | 66.731초, 분당 98.9토큰, 확정 필러 1회        | 반복되는 일에서 가치를 찾아 해결한다는 개발자 정체성과 세 사례, 마지막 메시지 회수가 좋았다. 다만 45~60초 구간이 빨라졌고 일부 문장이 여러 절을 한 호흡에 담았다. 사례의 문제 규모, 본인의 기여, 확인된 결과와 앞으로 할 일을 구분하면 시니어 후속 질문에 더 강해진다.                 | [전체 피드백](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/elevator-speech-2026-08-30-1/feedback.md) · [전달 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/elevator-speech-2026-08-30-1/delivery-review.json) · [논리 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/elevator-speech-2026-08-30-1/logic-review.json)             |
| 꼬리질문 무대본 답변 | 128.235초, 분당 92.64토큰, 확정 필러 `이제` 7회 | 질문 범위와 실무 사례는 드러났지만 결론보다 사례가 먼저 나왔고 `이런`, `그런`, `경우`가 범주 경계를 흐렸다. 논리적으로는 비결정성을 같은 조건으로 만드는 통제와 반복 실행으로 문제를 찾는 탐지를 구분하지 못했고, 시간 통제와 팀 합의의 검증 근거·절차를 답하지 못했다.                  | [전체 피드백](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-response-2026-08-30-1/feedback.md) · [전달 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-response-2026-08-30-1/delivery-review.json) · [논리 검토 원본](https://github.com/woong7361/architecture-thinking/blob/main/.codex/skills/speech-rehearsal/runs/test-response-2026-08-30-1/logic-review.json)                   |


세 결과에서 공통으로 나온 개선 방향은 대본 전체를 외우는 대신 `결론 한 문장 → 근거 또는 사례 → 적용 범위와 한계 → 결론 회수`를 먼저 고정하는 것이다. 전달 측면에서는 필러 대신 짧게 멈추고 한 문장에 하나의 판단만 담는다. 논리 측면에서는 사례가 무엇을 입증하는지와 입증하지 못하는지를 나누고, 판단 기준과 확인 절차를 함께 말한다.

---

### 3. AI가 저자 영역을 넘본 사례와 판단

**결론부터 말하면, AI의 침범은 있었다.** AI가 새로운 생각을 제안한 것 자체가 문제는 아니었다. AI가 만든 내용을 내 경험이나 판단인 것처럼 본문에 넣은 것이 문제였다. 이를 막기 위해 세 가지 장치를 만들었다. AI가 볼 수 있는 정보를 제한하고, 본문을 내가 준 글감과 대조하고, AI의 새로운 생각은 본문이 아닌 `추가 제안`으로 분리했다.

#### 1. 내가 주지 않은 정보가 초안에 들어왔다

초안에는 다음 문장이 들어갔다.

> 생성기는 초안을 쓰고, critique는 약한 지점을 말하고, evaluator는 루브릭 축으로 점수를 낸다. **validator는 스키마, 길이, 금칙어, 필수 조건처럼 기계적으로 확인 가능한 항목만 본다.**

내가 준 글감에는 `validator`, `금칙어`, `필수 조건` 같은 표현이 없었다. 반면 이 표현들은 파이프라인의 소스 파일과 일치했다. 당시 AI가 저장소를 읽을 수 있는 환경에서 실행됐기 때문에, 소스 파일의 내용이 초안으로 들어왔을 가능성이 높다고 봤다. 파일 접근 로그가 없어 실제 접근 여부까지 확정할 수는 없다.

그래서 각 단계를 빈 디렉터리에서 실행하고 셸 접근을 막았다. 그 결과 글감에 없던 용어는 사라지고, 글감에 있던 내용은 남았다.. 평가 점수도 4.31에서 4.06으로 내려갔다. 격리 때문에 점수가 낮아졌다고 단정할 수는 없지만, 입력에 없던 구체적인 표현이 사라지자 점수도 함께 낮아졌다는 사실은 확인했다. 이 사례를 통해 **입력 범위는 프롬프트에만 적지 말고 실행 환경에서도 강제해야 한다**고 판단했다.

#### 2. AI가 내 경험과 감정을 만들어냈다

실행 환경을 격리한 뒤에도 다른 문제가 남았다. AI는 "판정 장치를 만들면 마음이 편해질 줄 알았다", "사람이 직접 짠 코드라면 적어도 왜 그렇게 했는지 기억이라도 남는다", "처음에는 빠르게 보였다"고 썼다. 모두 내가 준 글감에 없으며, 내가 실제로 느꼈다고 확인한 적도 없는 내용이었다.

기존 평가는 이를 잡지 못했다. 오히려 근거 없는 문장이 네 건 있는 초안이 근거 추적 항목에서 4.8점, 그런 문장이 없는 초안이 4.5점을 받았다. 구체적인 문장은 사실이 아니어도 그럴듯해 보일 수 있기 때문이다.

그래서 점수에 맡기지 않고, 비평 단계에서 모든 1인칭 서술을 원래 글감과 대조하도록 바꿨다. 같은 방식으로 검사하자 문제가 있는 초안에서는 근거 없는 서술 4건을 찾았고, 깨끗한 초안에서는 0건을 찾았다.  이 사례를 통해 **저자의 경험과 감정은 품질 점수가 아니라 원재료와의 대조로 확인해야 한다**고 판단했다.

#### 3. 금지해도 계속 생긴 내용을 `추가 제안`으로 보냈다

AI는 내가 주지 않은 `계약 게이트`의 설명을 계속 만들어냈다. 글감에 없는 내용을 쓰지 말라는 금지 조건을 넣고 세 번 다시 생성했지만, 표현만 달라질 뿐 새로운 설명은 빠지지 않았다.

처음에는 이 내용을 모두 없애야 한다고 생각했다. 하지만 AI가 새로 만든 내용은 틀릴 수도 있지만, 내가 미처 생각하지 못한 반론이나 관점이 될 수도 있었다. 새로 만드는 것 자체보다 **내가 검토하지 않은 내용이 사실처럼 본문에 들어가는 것**이 문제였다.

그래서 새로운 내용을 만들지 못하게 하는 대신, 본문 밖의 `추가 제안`으로 보내도록 바꿨다. 저자의 확인이 필요한 사실이나 경험은 필요한 자료만 요청하게 하고, AI가 만든 반론이나 관점은 제안이라고 표시하게 했다. 그러자 글감에 없던 내용은 본문에서 빠졌고, 검토할 수 있는 제안으로 따로 나왔다. 실제로 다음과 같은 반론이 `추가 제안`에 담겼다.

 별도 제안으로 받으니 내가 검토한 뒤 채택하거나 버릴 수 있었다.  **AI가 새로 만든 내용을 무조건 금지하기보다, 제안으로 분리하고 최종 채택 여부를 내가 결정하는 편이 저자 영역을 더 잘 지킨다**고 판단했다.

세 사례를 거치며 D-0에서 세운 **"AI는 후보를 넓히는 데까지, 채택과 서열은 내가 한다"**는 원칙을 실제 구조로 옮겼다. 입력 밖의 정보는 실행 환경에서 막고, 내 경험과 판단은 원재료와 대조하고, AI의 생각은 본문 밖에서 내 승인을 기다리게 했다.

물론 비용도 있다. 실행 환경을 격리하면 AI가 활용할 수 있는 정보가 줄고, 원재료 대조를 넣으면 검토 단계가 늘어난다. 추가 제안을 직접 확인해야 하는 부담도 내게 남는다. 그래도 이 방식을 택했다. 저자 영역을 지킨다는 것은 AI의 제안을 모두 막는 일이 아니라, **AI의 제안을 검증하고 최종 선택권을 내가 갖는 일**이라고 판단했기 때문이다.

