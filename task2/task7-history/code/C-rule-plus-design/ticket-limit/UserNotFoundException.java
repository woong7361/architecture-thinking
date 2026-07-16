package com.thinking.ticket;

/**
 * 등록되지 않은 회원이 예매를 시도했다.
 *
 * <p>"실재하는 회원에게만 예매"는 예매의 업무 전제조건이므로, 저장소 제약(FK)에 떠넘기지 않고
 * 도메인 의미를 가진 예외로 알린다.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long userId) {
        super("등록되지 않은 회원입니다: " + userId);
    }
}
