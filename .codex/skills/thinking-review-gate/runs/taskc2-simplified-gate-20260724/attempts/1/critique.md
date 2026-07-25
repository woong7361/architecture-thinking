## 문제 지점

- [`P`의 annotation 없는 결합 처리] 초안은 XML 매핑, mapped superclass, converter를 검사 범위에 넣겠다고 하지만, `P=WEAK`은 약한 신호가 서로 다른 두 종류 이상일 때만 성립한다. 그래서 XML 매핑 하나만으로 업무 모델이 영속성 설정에 강하게 묶인 사례는 여전히 `P=NONE`이 될 수 있다. 이는 "annotation import가 없는 영속성 결합을 놓친다"는 문제 제기와 단순화안의 처리 사이에 약한 불일치가 있다.

- [`D=false -> COMBINE` 단정] 최종 결정표에서는 강제 architecture rule을 먼저 검사하지만, `D` 섹션에서는 `D=false -> COMBINE`을 독립 규칙처럼 제시한다. 읽는 사람이 강제 분리 규칙이 있어도 업무 규칙이 없으면 통합이라고 오해할 수 있다.

- [자동 판정 증거 범위] "모든 자동 판정 신호에는 `file:line`"을 요구하지만, `P=STRONG`의 proxy 초기화, persistence context 필요성, 실패 테스트 같은 신호는 실행 결과나 테스트 실패 로그가 근거가 될 수 있다. 현재 출력 계약은 파일 위치만 요구해 실행 결과 기반 증거를 어떻게 남길지 모호하다.

- [`M=HIGH -> SELECTIVE_REDESIGN`의 구체성] 높은 Mapper 위험을 통합이 아니라 재설계로 보내는 방향은 좋지만, 사용자가 "합칠 수 있는 것은 합쳐달라"고 요청한 맥락에서는 어떤 경우에 실제 통합을 선택하고 어떤 경우에 command model 부분 분리 같은 선택을 하는지 경계가 조금 더 필요하다. 지금은 `SELECTIVE_REDESIGN`이 합치기와 분리 사이의 보류 판정처럼 보일 수 있다.

- [예시의 검증 가능성] 예시는 5개로 충분하지만 대부분 축 값만 설명하고, 어떤 코드 신호가 그 값을 만들었는지의 evidence anchor 형식까지 보여주지는 않는다. success criteria의 "AI가 증거와 함께 판정"을 더 강하게 만족하려면 예시 중 하나는 `file:line` 형태의 샘플 출력까지 연결하는 편이 좋다.

## 확인 필요

- XML 매핑, mapped superclass, converter 같은 annotation 없는 매핑 결합을 약한 신호 1개로만 볼지, 특정 조건에서는 단독으로 `P=WEAK` 또는 `P=STRONG`으로 볼지 결정이 필요하다.

- `SELECTIVE_REDESIGN`이 최종 답변에서 "분리/통합 판정 실패"가 아니라 "현재 경계 그대로 분리하면 위험하므로 분리 범위를 줄이거나 경계를 다시 잡으라는 판정"임을 유지할지 확인이 필요하다.

- 실행 결과, 테스트 실패, 빌드 rule 같은 비소스 근거를 AI 출력 계약의 `signals`에 어떤 형식으로 남길지 확인이 필요하다.

## 수정 제안

- `P` 규칙에서 annotation 없는 결합을 실제로 잡도록 보강하라. 예를 들어 "업무 모델이 ORM XML, mapped superclass, custom converter로 직접 매핑되고 그 매핑 세부가 업무 method의 객체 구조나 lifecycle에 영향을 주면 단독으로 `P=WEAK`"처럼 결정에 영향을 주는 조건을 명시하면 된다.

- `D` 섹션의 `D=false -> COMBINE` 앞에 "최소 검사와 강제 architecture rule 검사를 통과한 뒤"라는 전제를 붙여라.

- 출력 계약의 evidence 형식을 `file:line`만이 아니라 `file:line`, `test result`, `build rule`, `runtime observation` 중 하나로 남길 수 있게 확장하라. 단, 자동 판정에 쓰는 근거와 참고 warning은 계속 분리해야 한다.

- `SELECTIVE_REDESIGN` 설명에 대표 선택지를 2~3개만 붙여라. 예: Aggregate 경계 축소, command model만 분리, query는 projection 유지. 이미 일부 설명이 있으므로 결정표 바로 아래에 짧게 고정하면 충분하다.

- 예시 하나를 YAML 출력 계약 형태로 바꿔라. 모든 예시를 길게 만들 필요는 없고, 하나만 evidence anchor가 붙은 샘플로 보여주면 AI가 어떻게 판정해야 하는지 더 검증 가능해진다.

## 요약

초안은 사용자의 핵심 요구를 대부분 충족한다. 기존 게이트가 놓치는 사례를 구체화했고, `I/T/A/H/S/C`를 `D/P/M` 세 축으로 줄인 방향도 Task C-1의 의존성 결론과 잘 맞는다. 다만 annotation 없는 매핑 결합이 실제 결정에서 다시 빠질 수 있고, `D=false`, `SELECTIVE_REDESIGN`, evidence 출력 형식의 경계가 약간 모호하다. 이 세 부분만 다듬으면 더 일관된 게이트가 된다.