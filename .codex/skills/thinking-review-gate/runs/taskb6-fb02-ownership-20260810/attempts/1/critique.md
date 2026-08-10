## 문제 지점

- [근거 anchor] 초안은 `pipeline/prompts/critique_refactor.md`, `pipeline/prompts/diagnose_refactor.md`, `pipeline/schemas/critique_output.schema.json`, `pipeline/runner.py`를 근거로 사용하지만, 답변 안에서는 이 근거가 `Checked Context`에서 확인된 내용인지 실제 파일을 다시 확인한 내용인지 구분하지 않는다. 사용자가 검증 가능한 답변이 되려면 "이번 판정은 input.md의 Checked Context에 기록된 확인 결과를 근거로 한다"는 경계를 분명히 해야 한다.

- [결정 지점] 사용자의 질문은 "critique 문제인지 gen 문제인지 확인을 통해 수정해야 할 문제로 결정"하는 것이다. 초안은 결론을 제시하지만, "분류상 주 책임은 Critique"와 "실제 수정 이슈는 Critique→Gen handoff contract"가 한 문단 안에서 섞여 보일 수 있다. 이 둘은 서로 다른 결정이므로 더 명시적으로 분리해야 한다.

- [수정 범위] 초안은 수정 대상을 `Critique output schema + runner handoff + Diagnose refine rule`로 잡는다. 이는 input의 판정과 일치하지만, "이번 단계는 파일 변경 없음"이라고 하면서도 다음 수정의 구체 범위가 넓다. 실제 다음 작업으로 넘기려면 우선순위를 `handoff contract와 validator 우선, prompt 보강은 보조`처럼 더 좁혀야 한다.

- [불확실성 경계] "이 결정으로 두 번째 피드백은 닫을 수 있다"는 표현은 남은 확인 항목이 없다는 강한 종료 선언처럼 읽힌다. 현재 input 기준으로는 결정이 가능하지만, 실제 파일 line-level 확인이나 테스트 재현까지 완료했다는 뜻은 아니다. "현재 제공된 Checked Context 기준으로는 닫을 수 있다"처럼 조건을 붙이는 편이 안전하다.

## 확인 필요

- `Checked Context`가 실제 파일 확인 결과를 충분히 담고 있는지, 아니면 draft가 최종 답변에서 파일 경로 수준의 anchor만 제공해도 되는지 확인이 필요하다.

- 다음 단계가 "FB-B6-03 검토로 이동"인지, 아니면 먼저 `Critique→Gen handoff contract` 수정 이슈를 별도 task로 등록하는 것인지 확인이 필요하다.

## 수정 제안

- 결론을 두 줄로 분리하라. 예: `분류: Critique 주 책임, Gen 보조 책임` / `수정 이슈: 미확인 가설이 handoff에서 구현 요구로 승격되는 계약 문제`.

- 근거 문단마다 "input.md Checked Context 기준" 또는 구체 파일 경로를 붙여 evidence anchor를 명시하라. 실제 line number가 없다면 line number를 만들지 말고 파일 단위 anchor로 제한하라.

- 수정 대상의 우선순위를 명확히 하라. 추천 순서는 `schema에서 confirmed_weaknesses/risk_hypotheses 분리` → `runner handoff gate` → `Diagnose refine rule` → `prompt 문구 보강`이다.

- 마지막 문장은 종료 조건을 좁혀라. "현재 확인된 문맥 기준으로 두 번째 피드백의 소유권 판정은 닫을 수 있다. 다만 실제 수정은 handoff contract task로 분리한다"처럼 쓰면 과도한 완료 선언을 피할 수 있다.

## 요약

초안의 핵심 판정은 input의 Checked Context와 대체로 일치한다. 다만 검증 가능한 답변으로 만들려면 근거의 출처 경계, 분류와 수정 이슈의 분리, 다음 단계의 우선순위, 종료 선언의 조건을 더 명확히 해야 한다.