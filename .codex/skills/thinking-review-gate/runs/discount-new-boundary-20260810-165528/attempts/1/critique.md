## 문제 지점

- [현재 코드 맥락 반영] 초안의 예시는 대부분 `PricingService` 기준으로 설명되어 있지만, 확인된 현재 문맥에는 `TicketService`가 저장소, 결제 포트, `DiscountPolicy`를 생성자로 주입받는다고 되어 있다. 직전 예시가 `PricingService`였기 때문에 완전히 틀린 것은 아니지만, 최종 답변에서는 "같은 원리가 현재 `TicketService`에도 적용된다"는 연결이 있어야 사용자가 자기 코드에 바로 대응시킬 수 있다.

- [`new`는 반드시 존재한다는 claim] "`new`는 시스템 어딘가에는 반드시 존재한다"는 표현은 방향은 맞지만 약간 단정적이다. Spring, reflection, serialization, test framework처럼 직접 `new`가 보이지 않는 경우도 있으므로, "직접 관리하는 애플리케이션 객체라면 구체 구현 선택 지점은 어딘가에 남는다"처럼 좁혀 쓰는 편이 더 검증 가능하다.

- [Factory 비교 부족] 입력 제약은 "수동 Composition Root, 런타임 Factory 또는 Resolver, Spring DI를 비교"하라고 되어 있는데, 초안은 Resolver는 비교하지만 Factory는 "단순히 `new`를 숨기기 위한 Factory" 정도로만 언급한다. 런타임 Factory가 필요한 경우와 Resolver와의 차이가 충분히 드러나지 않는다.

- [Composition Root 위치] `ApplicationConfiguration` 예시는 적절하지만, 현재 kata가 Spring 없는 작은 Java kata라는 점을 고려하면 실제 조립 위치가 `main`, 테스트 setup, CLI entry point, fixture 중 어디인지 불명확하다. "이름은 `ApplicationConfiguration`일 수도 있고, 현재 kata에서는 테스트나 실행 시작점의 조립 코드일 수도 있다"는 식의 범위 표시가 있으면 숨은 가정이 줄어든다.

## 확인 필요

- 현재 사용자가 묻는 대상이 직전 예시의 `PricingService`인지, 실제 코드의 `TicketService`인지 확인하면 예시명을 더 정확히 맞출 수 있다.

- 할인 정책이 애플리케이션 시작 시 하나로 고정되는지, 요청 값이나 사용자 조건에 따라 매번 달라지는지 확인하면 Resolver/Factory 필요성을 더 확정적으로 판단할 수 있다.

## 수정 제안

- 결론부나 첫 설명에 "`TicketService`가 이미 `DiscountPolicy` 인터페이스를 생성자로 받는다면 서비스 내부 분리는 된 상태이고, 남은 문제는 구체 정책을 어디서 조립하느냐"를 추가하라.

- "`new`는 반드시 존재한다"를 "`new` 자체보다 구체 구현 선택 지점을 어디에 둘지가 핵심"으로 완화하라.

- Resolver/Factory 섹션에 짧게 구분을 추가하라. 예를 들어 Resolver는 미리 만들어진 정책 중 하나를 고르고, Factory는 선택 시점에 새 객체 생성이나 생성 파라미터가 필요할 때 쓴다고 설명하면 된다.

- `ApplicationConfiguration`이 필수 클래스명처럼 보이지 않게 하고, 현재 kata에서는 수동 조립 코드의 예시 이름이라고 밝혀라.

## 요약

초안의 큰 결론은 현재 문맥과 잘 맞는다. 다만 실제 `TicketService` 맥락 연결, `new`에 대한 단정 완화, Factory와 Resolver의 차이 보강이 필요하다.