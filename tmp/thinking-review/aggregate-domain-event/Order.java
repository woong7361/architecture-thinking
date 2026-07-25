import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {
    private final OrderId id;
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(OrderId id) {
        this.id = Objects.requireNonNull(id);
    }

    public void addLine(
            OrderLineId lineId,
            ProductId productId,
            Money unitPrice,
            int quantity
    ) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("결제된 주문은 변경할 수 없다");
        }
        lines.add(new OrderLine(lineId, productId, unitPrice, quantity));
    }

    public void pay(Instant paidAt) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제된 주문이다");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("빈 주문은 결제할 수 없다");
        }

        status = OrderStatus.PAID;
        domainEvents.add(new OrderPaid(id, total(), paidAt));
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.ZERO, Money::add);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copied = List.copyOf(domainEvents);
        domainEvents.clear();
        return copied;
    }
}

record OrderId(UUID value) {
    OrderId {
        Objects.requireNonNull(value);
    }
}

record OrderLineId(UUID value) {
    OrderLineId {
        Objects.requireNonNull(value);
    }
}

record ProductId(UUID value) {
    ProductId {
        Objects.requireNonNull(value);
    }
}

record Money(BigDecimal amount) {
    static final Money ZERO = new Money(BigDecimal.ZERO);

    Money {
        Objects.requireNonNull(amount);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없다");
        }
    }

    Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 한다");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }
}

final class OrderLine {
    private final OrderLineId id;
    private final ProductId productId;
    private final Money unitPrice;
    private final int quantity;

    OrderLine(OrderLineId id, ProductId productId, Money unitPrice, int quantity) {
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 한다");
        }
        this.quantity = quantity;
    }

    Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}

sealed interface DomainEvent permits OrderPaid {
}

record OrderPaid(OrderId orderId, Money total, Instant occurredAt)
        implements DomainEvent {
    OrderPaid {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(total);
        Objects.requireNonNull(occurredAt);
    }
}

enum OrderStatus {
    DRAFT,
    PAID
}