package com.dmg.booking.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "booking",
        uniqueConstraints = @UniqueConstraint(name = "uq_booking_user_idem",
                columnNames = {"user_id", "idempotency_key"}))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "discount_code")
    private String discountCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Booking() {
    }

    public Booking(Long userId, Long showId, BigDecimal totalAmount, String idempotencyKey, String discountCode) {
        this.userId = userId;
        this.showId = showId;
        this.status = BookingStatus.CONFIRMED;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
        this.discountCode = discountCode;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getShowId() {
        return showId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
