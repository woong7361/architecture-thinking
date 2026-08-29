# Speech Rehearsal Feedback

- context: `7efb8b51809f`
- 공백 기준 발화 단위: 525
- 녹화 길이: 325.0초
- 분당 발화 단위: 96.92

## 필러 진단

- 확정 필러: 3회
- 의미 표현: 1회
- 판단 보류: 0회
- 분당 확정 필러: 0.55
- 확정 필러별 빈도: {"아니": 1, "이제": 2}

## 전달 진단

The speech has a clear delivery target: AI-era tests should function as a stable gate, not as vague reassurance. The opening and closing are relatively strong because they speak to the audience directly and end with a concrete action. The main delivery risk is the middle: generic transitions, repeated pronouns, and visible sentence repair make listeners reconstruct how each example supports the central point.

### filler_and_disfluency · caution

Fillers are not constant across the whole transcript, but they appear at important definition and example-entry moments. The repair at the refund example makes the setup sound improvised.

- 개선 행동: Rehearse the pivot sentences around "판정", "초록의 의미", and "예시" with a silent pause before each pivot instead of filling the gap.
- 근거: 이제 판정, 테스트 코드가

### pace_and_pause · not_evaluable

The context provides overall duration and an overall metric of 96.92 whitespace tokens per minute, but segment_pace is empty and the transcript does not preserve pause lengths.

- 개선 행동: For the next review, provide segment-level pace or audio-derived pause timestamps so pace can be diagnosed separately from wording.
- 근거: 总时长:           5分钟25秒

### verbal_style · problem

The delivery relies heavily on broad referents like "이것", "그것", and "그런 것들", plus stacked clauses ending in repeated "수도 있고" patterns.

- 개선 행동: Replace vague referents at key points with the noun you mean, especially "테스트 입력", "초록 결과", "판정 기준", and "사각지대".
- 근거: 사각지대는 언제든 나올 수 있습니다. 중간에 내가 작업을 하다 생각날 수도 있고 맞은 케이스가 혹은 다른 서드 파티 나 라이브러리 업데이트에서 갑자기 에러가 생길 수도 있고

### opening · good

The opening quickly names the topic and frames a real audience concern: AI-generated code and whether tests actually guarantee behavior.

- 개선 행동: Keep the audience-question opening, but end it with the central gate sentence so the following sections have a stronger anchor.
- 근거: 안녕하세요. AI 시대의 테스트에 대해서 설명을 해보려고 합니다.

### core_message · caution

The central message is identifiable, especially in the latter half, but it arrives after several question chains and example fragments.

- 개선 행동: Move the "흔들리지 않는 게이트" message into the first minute, then use each example as proof of that one sentence.
- 근거: 결론적으로 테스트란 것은 먼저 인상으로 결정한 것이 아니라 흔들리지 않는 절대적 게이트라는 것이 중요합니다.

### development_and_transitions · problem

Transitions use broad words like "그렇다면", "이들을", and "하지만" without always stating the relationship between the previous claim and the next example.

- 개선 행동: Before each example, say which claim it proves: instability of impression, limited meaning of green, or remaining blind spots.
- 근거: 항상 흔들리게 되죠. 그렇다면 이제 판정, 테스트 코드가 있다면 그것은 괜찮은 건가요?

### closing · good

The close gives a concrete next action and retrieves the gate idea from the core message.

- 개선 행동: Keep the closing action, but cleanly articulate the final sentence before "감사합니다" so the last takeaway is not blurred by the garbled phrase at t0510-t0511.
- 근거: 테스트 코드가 있다면 초록인 테스트 코드를 하나 골라서 부등호 하나를 집어보는 겁니다.

## 논리 진단

중심 주장은 대체로 식별된다. 테스트는 코드 전체의 안전을 보장하는 것이 아니라, 주어진 입력과 기대값에 대해 흔들리지 않는 판정 기준이어야 한다는 주장이다. 다만 사례가 그 주장의 어느 부분을 증명하는지, AI 평가가 어떻게 흔들렸는지, 결함 주입 결과를 어떻게 해석해야 하는지의 근거가 부족해 시니어 질문에는 아직 취약하다.

### thesis_identifiability · caution

주장은 보이지만 늦게 선명해진다. 특히 '절대적 게이트'라는 표현이 뒤의 한정된 보장 주장과 함께 들리며 범위가 모호해진다.

- 개선 행동: 초반에 테스트가 보장하는 것은 전체 안전이 아니라 특정 입력과 기대값에 대한 반복 가능한 판정이라는 한정된 thesis를 명시하라.
- 근거: 근데 테스트가 있다고 정말 안전한 걸까요?

### reasoning_chain · caution

큰 흐름은 따라갈 수 있지만 중간 연결고리가 충분히 명시되지 않는다. 청중이 이미 테스트 설계와 mutation testing의 의미를 알고 있어야 빈칸을 메울 수 있다.

- 개선 행동: 결함 주입이 '테스트가 실패해야 할 변경에서 실제로 실패하는지 확인하는 절차'라는 연결을 명시하고, 그 결과가 무엇을 말해주는지 분리해서 설명하라.
- 근거: 테스트의 초록 빨강은 그 테스트가 주어진 입력에 대해서 이제 동작을 보장한다는 것이지 모든 케이스에 대해서 그것이 안전하다는 것은 아닙니다.

### evidence_fit · caution

사례 방향은 주장과 맞지만 증거의 해상도가 낮다. 숫자는 나오지만 그 숫자가 정확히 어떤 claim을 지지하는지 확인하기 어렵다.

- 개선 행동: 각 사례마다 '무엇을 비교했는지', '무엇이 실패했는지', '그 실패가 어떤 결론을 지지하는지'를 한 줄씩 붙여라.
- 근거: 결과가 흔들리고 있는 거죠. 하지만 내가 만든 인스테스트 18종을 검증했을 때는 18개 중에 16개를 통과했고 예를 들어서 2개는 틀림 명확한 응답을 얻었다고 항상 동일한 결과와 피드백이 온 것이죠.

### assumptions_and_scope · problem

범위 설정이 가장 큰 약점이다. '절대적'이라는 단어가 테스트의 한정된 보장이라는 앞선 주장보다 강하게 들려 과대 주장으로 공격받기 쉽다.

- 개선 행동: 게이트의 범위를 '명세화된 입력과 기대값 안에서'로 제한하고, 그 밖의 사각지대는 별도의 확인이나 계약 테스트가 필요하다고 경계를 세워라.
- 근거: 사각지대는 언제든 나올 수 있습니다.

### consistency · caution

발표 내부에 직접 모순은 크지 않지만, 강한 표현과 한정 표현이 충돌해 들릴 수 있다.

- 개선 행동: '절대적'을 '반복 가능한' 또는 '정해진 범위 안에서 흔들리지 않는'으로 바꾸거나, 발표 안에서 같은 의미로 정의하라.
- 근거: 모든 케이스에 대해서 그것이 안전하다는 것은 아닙니다.

### conclusion_support · caution

실천 제안은 강하지만 해석 조건이 빠져 있다. 그대로 말하면 유효한 테스트를 잘못 버리거나, 범위 밖 변경을 테스트 실패 기준으로 삼는 위험을 설명하지 못한다.

- 개선 행동: 결론에서 '의도한 동작을 깨뜨리는 변경을 넣었는데도 초록이면'처럼 판단 조건을 붙여라.
- 근거: 테스트 코드가 있다면 초록인 테스트 코드를 하나 골라서 부등호 하나를 집어보는 겁니다. 혹은 입력을 바꿔보던가 코드를 조금 바꿔보는 겁니다.

### question_resilience · problem

핵심 증거가 검증 가능한 절차로 닫혀 있지 않아 후속 질문 압력이 크다.

- 개선 행동: 후속 질문을 받을 핵심 사례 하나만 골라 절차, 입력, 기대값, 결과, 결론을 검증 가능한 순서로 말하라.
- 근거: 환불 도메인에서 제가 Ai에게 테스트 코드를 짜달라고 하고 그 평가를 맡겨보았습니다. 두 가지로 맡겨보았는데요.

## 시니어 후속 질문

- AI 평가가 '흔들렸다'는 것은 정확히 무엇이 어떻게 달라졌다는 뜻인가요? 같은 코드와 같은 질문을 여러 번 넣었을 때 결과가 달라졌다는 말인지, 테스트와 AI 평가가 서로 다른 결과를 냈다는 말인지 구분해 설명할 수 있나요?
  - 이유: 발표의 핵심 대비가 인상 또는 AI 평가와 테스트 판정의 차이인데, 흔들림의 측정 방식이 없으면 테스트가 더 나은 게이트라는 결론이 약해진다.
- 초록 테스트에 부등호나 입력을 바꿨는데 계속 초록이면 언제 그 테스트를 '쓸모없다'고 판단할 수 있나요? 변경이 실제 요구사항을 깨뜨린 유효한 결함이라는 기준은 무엇인가요?
  - 이유: 마무리의 실천 제안은 강하지만, 결함 주입의 유효성 기준이 없으면 잘못된 변경으로 테스트 가치를 판단할 수 있다.
- '흔들리지 않는 절대적 게이트'라고 할 때 절대적인 범위는 어디까지인가요? 주어진 입력 안에서만 절대적인 것인지, 외부 라이브러리 업데이트나 서드파티 변경까지 포함하는 것인지 선을 그을 수 있나요?
  - 이유: 발표자는 테스트를 절대적 게이트라고 부르면서도 항상 안전한 것은 아니라고 한정하기 때문에, 범위를 묻는 질문이 바로 들어올 수 있다.

## 다음 리허설 행동

- Move the central gate sentence into the opening so listeners have one anchor before the examples begin.
- Add explicit signposts before each example: which claim the example proves and what the listener should notice.
- Replace vague referents like "이것", "그것", and "그런 것들" with concrete nouns at definition and transition points.
- 중심 주장을 '정해진 입력과 기대값 안에서 반복 가능한 판정 기준'으로 한정해서 다시 말하라.
- 환불 도메인 사례는 AI 평가 절차, 18개 테스트의 판정 기준, 실패 2개의 의미를 분리해 증거로 제시하라.
- 결함 주입 제안에는 유효한 결함 변경의 조건과 초록 결과를 해석하는 기준을 붙여라.

## 한계

- No audio was provided, so pitch, volume, emphasis, confidence, emotion, and audience reaction were not evaluated.
- segment_pace is empty, so pace and pause quality could not be responsibly diagnosed.
- The transcript appears machine-generated and contains possible recognition errors such as "인스테스트" and "일부는 투자여".
- 이 평가는 제공된 review-context.json의 transcript만 발화 증거로 사용했다.
- presentation_plan은 의도 파악에는 참고될 수 있지만 실제 발화의 증거로 사용하지 않았다.
- 원본 코드, 테스트 파일, AI 평가 로그가 없어 사례의 사실 여부와 재현성은 검증하지 못했다.
- 전사 오류 가능성이 있어 일부 어색한 표현은 발화 의도와 다를 수 있다.
