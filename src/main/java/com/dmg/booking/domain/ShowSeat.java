package com.dmg.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The bookable unit: one physical {@link Seat} for one {@link Show}.
 * This is the row guarded against double-booking (pessimistic lock on BOOK,
 * atomic conditional UPDATE on HOLD). {@code version} backs JPA optimistic locking.
 */
@Entity
@Table(name = "show_seat",
       uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}))
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(nullable = false)
    private BigDecimal price;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "hold_id")
    private Long holdId;

    @Column(name = "held_until")
    private Instant heldUntil;

    @Column(name = "booking_id")
    private Long bookingId;

    protected ShowSeat() {
    }

    public Long getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public Seat getSeat() {
        return seat;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getVersion() {
        return version;
    }

    public Long getHoldId() {
        return holdId;
    }

    public Instant getHeldUntil() {
        return heldUntil;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public void setHeldUntil(Instant heldUntil) {
        this.heldUntil = heldUntil;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}
