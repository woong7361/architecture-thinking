CREATE TABLE kata_users (
    id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- 현재 중복 판단은 서비스의 기존 신청 조회로 수행한다.
CREATE TABLE alert_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    court_id BIGINT NOT NULL,
    play_date DATE NOT NULL,
    time_slot VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES kata_users(id),
    INDEX idx_active_lookup (user_id, court_id, play_date, time_slot, status)
) ENGINE=InnoDB;
