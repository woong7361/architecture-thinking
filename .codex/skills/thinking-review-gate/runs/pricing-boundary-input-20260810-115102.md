# Original User Input

위의 가격 계산 서비스가 규칙들을 조합하는 편이 낫다 

이거는 어떻게 한다는거야? 예시를 보여줘 

공통 pricingContext를 쓴다는 것도 


그러면 결국 공통화에 따라 경계를 나누어 잡아야한다는거네

# Confirmed Context

- 현재 `TicketService`는 `DiscountPolicy.finalAmount(ticket.getPrice())`로 기준 가격 하나만 전달한다.
- 현재 `DiscountPolicy`는 5만 원 이상일 때 10%를 할인하는 구체 클래스다.
- VIP 등급과 외부 쿠폰 서버는 FB-B5-01이 제시한 가정이며 확정 요구가 아니다.
- 사용자는 좁은 규칙 조합 방식과 공통 `PricingContext` 방식의 구체적인 차이를 묻고 있다.

# Constraints

- 현재 프로젝트 코드를 수정하지 않는다.
- Java 예시는 현재 선호 언어와 Java 17 문맥에 맞춘다.
- 외부 서버 DTO가 도메인 계산에 직접 들어가지 않도록 한다.
- 예시의 할인 순서는 설명을 위한 가정이라고 명시한다.
- 두 접근의 경계, 데이터 흐름, trade-off를 비교한다.
