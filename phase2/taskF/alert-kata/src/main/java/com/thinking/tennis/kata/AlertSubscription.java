package com.thinking.tennis.kata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "alert_subscriptions")
public class AlertSubscription {
    public static final String WATCHING = "WATCHING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long userId;
    private long courtId;
    private LocalDate playDate;
    @Column(length = 32)
    private String timeSlot;
    @Column(length = 16)
    private String status;

    protected AlertSubscription() {
    }

    public AlertSubscription(SubscribeCommand command) {
        userId = command.userId();
        courtId = command.courtId();
        playDate = command.playDate();
        timeSlot = command.timeSlot();
        status = WATCHING;
    }

    public Long id() {
        return id;
    }
}
