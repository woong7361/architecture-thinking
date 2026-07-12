# OBJECT 
글 조영호


구체 클래스를 너무 의존하면 수정하기가 너무 어렵다. 내부 구현을 전부 알고있어야한다.
-> 자율적인 존재로 만들면 좋겠다
내부 제한으로(캡슐화) 변경 용이성이 확실히 개선

전부 트레이드 오프다 자율성을 올리면 의존성또한 같이 올라갈 수 있다. - 고려해서 해야한다.

훌륭함 - 객체사이의 의존성을 잘 관리


2장
객체지향 프로그래밍
설계를 할때 클래스가 아닌 객체를 중심으로 생각 - 어떤 객체들이 어떤 상태와 행동을 가지는지 결정, 객체를 독립적인 것이 아니라 협력적인 존재로 생각

항상 예외 케이스를 최소화하고, 일관성을 지키는 방식을 유지하라


## 추상 클래스 vs 인터페이스 트레이드오프 (할인 정책 예제)

### 추상 클래스를 쓴 이유
- 할인 정책들(AmountDiscountPolicy, PercentDiscountPolicy)의 공통 로직(조건 순회)을 재사용하려고 → TEMPLATE METHOD 패턴
- 변하는 부분(할인 금액 계산)만 자식에게 위임

```java
public abstract class DiscountPolicy {
    private List<DiscountCondition> conditions = new ArrayList<>();

    // 공통 골격 (변하지 않는 부분) — 재사용됨
    public Money calculateDiscountAmount(Screening screening) {
        for (DiscountCondition each : conditions) {
            if (each.isSatisfiedBy(screening)) {
                return getDiscountAmount(screening); // 변하는 부분에 위임
            }
        }
        return Money.ZERO;
    }

    // 변하는 부분 (자식이 채움)
    abstract protected Money getDiscountAmount(Screening screening);
}
```

### 인터페이스로 바꾼 이유
- "할인 없음(NoneDiscountPolicy)"은 부모의 조건 순회 로직이 필요 없음 → 억지 상속 발생
- 예외적인 구현체를 불필요한 부모 구현으로부터 분리하기 위해
- 최종 형태: 역할은 인터페이스(DiscountPolicy), 공통 구현은 추상 클래스(DefaultDiscountPolicy), 예외는 인터페이스 직접 구현

```java
// 순수한 계약(역할)만 정의
public interface DiscountPolicy {
    Money calculateDiscountAmount(Screening screening);
}

// 공통 구현이 필요한 정책들만을 위한 추상 클래스
public abstract class DefaultDiscountPolicy implements DiscountPolicy {
    private List<DiscountCondition> conditions = new ArrayList<>();

    @Override
    public Money calculateDiscountAmount(Screening screening) {
        for (DiscountCondition each : conditions) {
            if (each.isSatisfiedBy(screening)) {
                return getDiscountAmount(screening);
            }
        }
        return Money.ZERO;
    }
    abstract protected Money getDiscountAmount(Screening screening);
}

public class AmountDiscountPolicy extends DefaultDiscountPolicy { ... }
public class PercentDiscountPolicy extends DefaultDiscountPolicy { ... }

// 공통 구현이 필요 없는 예외적인 정책은 인터페이스를 직접 구현
public class NoneDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculateDiscountAmount(Screening screening) {
        return Money.ZERO; // 순회 없이, 자기 방식대로
    }
}
```

### 트레이드오프
- 추상 클래스 = 구현 재사용(중복 제거) ↔ 부모 구현에 강하게 결합, 단일 상속만 가능
- 인터페이스 = 역할의 유연성(자유로운 구현) ↔ 공통 코드 재사용 불가
- → 둘을 함께 써서 재사용성과 유연성을 모두 취함

### "예외 최소화·일관성" 원칙과의 연결
- null 분기 대신 NoneDiscountPolicy 객체를 둬서 Movie가 모든 정책을 똑같이 취급 (예외 케이스 제거)
- 일관성을 위해 예외 케이스도 같은 역할(인터페이스)로 취급하되, 맞지 않는 상속은 강요하지 않음
- 즉, 인터페이스로 바꾼 진짜 목적이 바로 이 원칙(예외 최소화 + 일관성 유지)

**나쁜 예 — null 분기 (예외 케이스가 코드에 노출됨)**
```java
public class Movie {
    private DiscountPolicy discountPolicy; // 할인 없으면 null

    public Money calculateMovieFee(Screening screening) {
        if (discountPolicy == null) {   // ← 예외 케이스 분기
            return fee;                 //   할인 정책 쓰는 모든 코드가 반복해야 함
        }                               //   빠뜨리면 NullPointerException
        return fee.minus(discountPolicy.calculateDiscountAmount(screening));
    }
}
```

**좋은 예 — NoneDiscountPolicy 객체 (예외를 일반 케이스로 흡수)**
```java
public class Movie {
    private DiscountPolicy discountPolicy; // 이제 절대 null 아님

    public Money calculateMovieFee(Screening screening) {
        // 분기 없음. 모든 정책을 똑같이 대한다.
        return fee.minus(discountPolicy.calculateDiscountAmount(screening));
    }
}
```