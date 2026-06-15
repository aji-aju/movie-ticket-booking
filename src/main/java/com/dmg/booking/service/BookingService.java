package com.dmg.booking.service;

import com.dmg.booking.domain.Booking;
import com.dmg.booking.domain.HoldStatus;
import com.dmg.booking.domain.Payment;
import com.dmg.booking.domain.PaymentStatus;
import com.dmg.booking.domain.SeatHold;
import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.ForbiddenException;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.BookingRepository;
import com.dmg.booking.repository.PaymentRepository;
import com.dmg.booking.repository.SeatHoldRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class BookingService {

    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final PaymentGateway paymentGateway;

    public BookingService(ShowSeatRepository showSeatRepository,
                          BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          SeatHoldRepository seatHoldRepository,
                          PaymentGateway paymentGateway) {
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Confirm a booking for seats held by the caller's own hold. Correctness under
     * concurrency comes from pessimistically locking the seat rows (SELECT ... FOR UPDATE).
     * Security: the hold must belong to the authenticated user (no booking against someone
     * else's hold), and idempotency replay is scoped per-user. A concurrent same-key insert
     * hits the unique constraint and is mapped to 409 by the global handler.
     */
    @Transactional
    public BookingResponse book(Long userId, String idempotencyKey, BookingRequest request) {
        // 1. Idempotent replay — scoped to the caller so a key can't read another user's booking.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // 2. The hold must exist and belong to the caller (prevents IDOR: booking another user's hold).
        SeatHold hold = seatHoldRepository.findById(request.holdId())
                .orElseThrow(() -> new NotFoundException("Hold " + request.holdId() + " not found"));
        if (!hold.getUserId().equals(userId)) {
            throw new ForbiddenException("Hold " + request.holdId() + " does not belong to the current user");
        }

        // 3. Lock the seat rows (FOR UPDATE) — the serialization point.
        List<ShowSeat> seats = showSeatRepository.lockSeatsForUpdate(request.showSeatIds());
        if (seats.size() != request.showSeatIds().size()) {
            throw new NotFoundException("One or more seats do not exist");
        }

        // 4. Every seat must still be HELD by THIS hold and not expired.
        Instant now = Instant.now();
        for (ShowSeat seat : seats) {
            boolean heldByThisHold = seat.getStatus() == SeatStatus.HELD
                    && request.holdId().equals(seat.getHoldId())
                    && seat.getHeldUntil() != null
                    && seat.getHeldUntil().isAfter(now);
            if (!heldByThisHold) {
                throw new ConflictException("Seat " + seat.getId()
                        + " is not held by this hold or the hold has expired");
            }
        }

        // 5. Create the booking (showId comes from the verified hold).
        BigDecimal total = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking = bookingRepository.save(
                new Booking(userId, hold.getShowId(), total, idempotencyKey));

        // 6. Flip the locked seats to BOOKED.
        for (ShowSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBookingId(booking.getId());
            seat.setHeldUntil(null);
        }

        // 7. Take payment (mock) and convert the hold.
        String ref = paymentGateway.charge(booking.getId(), total);
        paymentRepository.save(new Payment(booking.getId(), total, PaymentStatus.SUCCESS, ref));
        hold.setStatus(HoldStatus.CONVERTED);

        return new BookingResponse(booking.getId(), booking.getStatus().name(),
                total, request.showSeatIds(), booking.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> history(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private BookingResponse toResponse(Booking booking) {
        List<Long> seatIds = showSeatRepository.findByBookingId(booking.getId()).stream()
                .map(ShowSeat::getId)
                .toList();
        return new BookingResponse(booking.getId(), booking.getStatus().name(),
                booking.getTotalAmount(), seatIds, booking.getCreatedAt());
    }
}
