package com.dmg.booking.service;

import com.dmg.booking.domain.Booking;
import com.dmg.booking.domain.HoldStatus;
import com.dmg.booking.domain.Payment;
import com.dmg.booking.domain.PaymentStatus;
import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.exception.ConflictException;
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
     * Confirm a booking for seats currently held by the caller's hold. Correctness
     * under concurrency comes from pessimistically locking the seat rows
     * (SELECT ... FOR UPDATE): concurrent attempts on the same seat serialize here,
     * so exactly one wins and the rest see a non-HELD seat and get a 409.
     * An idempotency key makes a retried submit return the original booking.
     */
    @Transactional
    public BookingResponse book(Long userId, String idempotencyKey, BookingRequest request) {
        // 1. Idempotent replay: a retried submit returns the original booking.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // 2. Lock the seat rows (FOR UPDATE) — the serialization point.
        List<ShowSeat> seats = showSeatRepository.lockSeatsForUpdate(request.showSeatIds());
        if (seats.size() != request.showSeatIds().size()) {
            throw new NotFoundException("One or more seats do not exist");
        }

        // 3. Every seat must still be HELD by THIS hold and not expired.
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

        // 4. Create the booking (total = sum of seat prices; pricing strategies layer in M6).
        BigDecimal total = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long showId = seats.get(0).getShow().getId();
        Booking booking = bookingRepository.save(new Booking(userId, showId, total, idempotencyKey));

        // 5. Flip the locked seats to BOOKED.
        for (ShowSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBookingId(booking.getId());
            seat.setHeldUntil(null);
        }

        // 6. Take payment (mock) and record it.
        String ref = paymentGateway.charge(booking.getId(), total);
        paymentRepository.save(new Payment(booking.getId(), total, PaymentStatus.SUCCESS, ref));

        // 7. Convert the hold.
        seatHoldRepository.findById(request.holdId())
                .ifPresent(h -> h.setStatus(HoldStatus.CONVERTED));

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
