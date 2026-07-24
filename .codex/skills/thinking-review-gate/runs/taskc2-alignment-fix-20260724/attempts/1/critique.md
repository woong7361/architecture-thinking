## 문제 지점

- [D/P 정의] 초안은 `D`, `P`, `D AND P`를 핵심 판단 기준으로 사용하지만, 본문 안에서 D와 P의 뜻을 다시 정의하지 않는다. 입력 컨텍스트에는 D가 “보호할 업무 규칙”, P가 “JPA 때문에 테스트·변경 격리가 깨지는 확인 가능한 증거”라고 정리되어 있으므로, 최종 답안에도 이 정의를 먼저 넣어야 독자가 판단 기준을 따라갈 수 있다.

- [근거 anchor] 초안은 Task C-2 요구사항, Task C-1의 문제의식, 직전 합의를 반영하고 있지만 본문 안에는 확인 가능한 파일 위치나 요구 문구 anchor가 거의 없다. 사용자가 “산출물에서 요구하는” 항목 누락을 지적했으므로, `task3/assignments/taskC-2.md:7-8`의 요구가 “분리 방식과 통합 방식 조사, 현실적 장단점, 개인 기준”이라는 점을 짧게 연결해야 한다.

- [의존 방향 다이어그램] `Application Core <- Port <- TicketPersistenceAdapter -> JPA Repository`는 화살표가 호출 방향인지 컴파일 의존 방향인지 불명확하다. 특히 `Port <- TicketPersistenceAdapter`는 “Adapter가 Port를 구현하고 의존한다”는 뜻으로 읽힐 수 있지만, 동시에 흐름 방향처럼 보이면 혼동된다. `Domain Ticket <------ TicketMapper ------> TicketJpaEntity`도 Domain이 Mapper에 의존하는 것처럼 오해될 여지가 있다.

- [분리 방식의 배치] 분리 구조는 잘 제시되어 있지만 `LoadTicketPort`와 `SaveTicketPort`가 application core에 있고, `TicketPersistenceAdapter`가 그 port를 구현한다는 점을 명시적으로 한 문장 더 고정하면 좋다. 현재도 추론 가능하지만, 사용자가 요구한 “분리하는 방식”을 충족하려면 코드 배치와 의존 규칙이 더 분명해야 한다.

- [사례 범위] PetClinic, Buckpal, Netflix, Shopify 사례에 대해 “무엇을 보여주는 사례인지”는 제한하고 있으나, 이 사례들이 이번 교정 결론의 직접 근거인지 보조 예시인지 구분이 약하다. 공개 사례는 보조 예시이고 최종 판단 근거는 과제 요구와 D/P 기준이라는 점을 더 분명히 해야 한다.

## 확인 필요

- 최종 답안이 실제 과제 파일에 반영될 때, 기존 답안의 어느 섹션을 교체할지 확인해야 한다. 현재 초안은 “교체할 현장·사이즈 기준”과 “추가할 분리 방식”은 제공하지만, 대상 파일의 정확한 삽입 위치나 기존 문단 제거 범위는 아직 드러나지 않는다.

- `Ticket` 예시가 기존 과제 답안의 도메인 예시와 일치하는지 확인해야 한다. 입력 컨텍스트는 “Ticket 예시는 설명용”이라고 제한하지만, 기존 답안이 다른 예시를 쓰고 있다면 예시 통일이 필요하다.

## 수정 제안

- 시작 부분에 D/P 정의를 추가한다. 예: “여기서 D는 분리해서 보호할 업무 규칙이 있는지, P는 JPA 모델 때문에 테스트나 변경 격리가 실제로 깨지는지다.”

- Task C-2 요구와 연결하는 문장을 추가한다. 예: “과제 요구가 분리·통합 방식의 현실적 장단점과 개인 기준을 묻기 때문에, 표는 규모별 정답표가 아니라 D/P를 확인하는 보조 문맥으로 바꿔야 한다.”

- 의존 방향 다이어그램은 화살표 의미를 라벨링하거나 아래처럼 더 명확히 바꾼다.

```text
컴파일 의존 방향:
adapter/out/persistence -> application/port/out -> domain
adapter/out/persistence -> JPA

런타임 호출 흐름:
Application Service -> Load/Save Port -> TicketPersistenceAdapter -> JpaRepository
```

- Mapper 설명에서 “Mapper는 adapter 패키지에 있고 Domain은 Mapper와 JPA Entity를 모른다”는 문장을 추가한다.

- 공개 사례 문단은 “보조 예시”로 낮추고, 과제 답안의 핵심 근거는 `D/P 기준`과 `Task C-2 요구사항`임을 명시한다.

## 요약

초안은 사용자의 핵심 지적을 대부분 반영한다. 현장·사이즈 기준을 D/P의 보조 문맥으로 낮추고, 분리 구조와 load/save 흐름도 제시했다. 다만 D/P 정의, 과제 요구사항 anchor, 의존 방향 표기의 명확성이 부족해 최종 답안으로 쓰기 전 이 부분을 보강해야 한다.