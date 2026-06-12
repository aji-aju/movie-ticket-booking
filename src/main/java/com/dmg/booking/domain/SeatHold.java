package com.dmg.booking.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "seat_hold")
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected SeatHold() {
    }

    public SeatHold(Long showId, Long userId, Instant expiresAt) {
        this.showId = showId;
        this.userId = userId;
        this.status = HoldStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Long getShowId() {
        return showId;
    }

    public Long getUserId() {
        return userId;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }
}
