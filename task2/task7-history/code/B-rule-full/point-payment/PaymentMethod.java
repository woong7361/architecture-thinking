package com.thinking.ticket;

/**
 * 티켓 값을 받아내는 방법.
 *
 * <p>누가 내는지·무엇으로 내는지는 각 구현이 만들어질 때 이미 정해져 있다. 여기 남는 요청은 금액뿐이다.
 *
 * <p>계약: {@code pay}가 정상으로 돌아오면 그 금액은 받아진 것이다. 받아내지 못하면 거절 사유를 알리는
 * 예외를 던지고, 이때 어떤 청구도 차감도 남지 않는다. 어느 구현이든 이 약속은 같다.
 */
interface PaymentMethod {

    void pay(int amount);
}
