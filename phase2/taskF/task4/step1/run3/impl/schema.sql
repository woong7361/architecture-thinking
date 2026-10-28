CREATE TABLE kata_users (
    id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- 중복 판단은 서비스의 조회와 아래 유일 제약이 함께 보장한다.
-- watching_marker 는 감시 중인 행에서만 1 이고 그 밖에는 NULL 이다.
-- MySQL 의 유일 인덱스는 NULL 을 서로 다른 값으로 보므로
-- 감시 중인 신청만 (사용자, 코트, 날짜, 시간대) 하나로 제한되고
-- 종료된 신청은 같은 조건으로 몇 건이든 남을 수 있다.
CREATE TABLE alert_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    court_id BIGINT NOT NULL,
    play_date DATE NOT NULL,
    time_slot VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    watching_marker TINYINT
        GENERATED ALWAYS AS (CASE WHEN status = 'WATCHING' THEN 1 ELSE NULL END) STORED,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES kata_users(id),
    CONSTRAINT uq_watching_subscription
        UNIQUE (user_id, court_id, play_date, time_slot, watching_marker),
    INDEX idx_active_lookup (user_id, court_id, play_date, time_slot, status)
) ENGINE=InnoDB;
