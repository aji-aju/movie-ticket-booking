package com.dmg.booking.service;

import com.dmg.booking.domain.Booking;
import com.dmg.booking.domain.BookingStatus;
import com.dmg.booking.domain.HoldStatus;
import com.dmg.booking.domain.Payment;
import com.dmg.booking.domain.PaymentStatus;
import com.dmg.booking.domain.SeatHold;
import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.Show;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.CancelResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.ForbiddenException;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.BookingRepository;
import com.dmg.booking.repository.PaymentRepository;
import com.dmg.booking.repository.SeatHoldRepository;
import com.dmg.booking.repository.ShowRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.event.BookingConfirmedEvent;
import com.dmg.booking.strategy.PricingStrategy;
import com.dmg.booking.strategy.RefundStrategy;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ShowRepository showRepository;
    private final PaymentGateway paymentGateway;
    private final PricingStrategy pricingStrategy;
    private final DiscountService discountService;
    private final RefundStrategy refundStrategy;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(ShowSeatRepository showSeatRepository,
                          BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          SeatHoldRepository seatHoldRepository,
                          ShowRepository showRepository,
                          PaymentGateway paymentGateway,
                          PricingStrategy pricingStrategy,
                          DiscountService discountService,
                          RefundStrategy refundStrategy,
                          ApplicationEventPublisher eventPublisher) {
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.showRepository = showRepository;
        this.paymentGateway = paymentGateway;
        this.pricingStrategy = pricingStrategy;
        this.discountService = discountService;
        this.refundStrategy = refundStrategy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BookingResponse book(Long userId, String idempotencyKey, BookingRequest request) {
        // 1. Idempotent replay — scoped to the caller.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // 2. The hold must exist and belong to the caller (no IDOR).
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

        // 5. Price (tier + weekend) then apply any discount code.
        Show show = showRepository.findById(hold.getShowId())
                .orElseThrow(() -> new NotFoundException("Show " + hold.getShowId() + " not found"));
        BigDecimal subtotal = seats.stream()
                .map(seat -> pricingStrategy.priceFor(seat, show))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = discountService.applyDiscount(subtotal, request.discountCode());

        Booking booking = bookingRepository.save(
                new Booking(userId, hold.getShowId(), total, idempotencyKey, request.discountCode()));

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

        // Confirmation is dispatched asynchronously after this tx commits (non-blocking).
        eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId(), userId, total));

        return new BookingResponse(booking.getId(), booking.getStatus().name(),
                total, request.showSeatIds(), booking.getCreatedAt());
    }

    /**
     * Cancel a booking the caller owns: release its seats back to the pool, compute a
     * refund per the time-based {@link RefundStrategy}, and record the refund.
     */
    @Transactional
    public CancelResponse cancel(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException("Booking " + bookingId + " does not belong to the current user");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + bookingId + " is already cancelled");
        }

        Show show = showRepository.findById(booking.getShowId())
                .orElseThrow(() -> new NotFoundException("Show " + booking.getShowId() + " not found"));
        BigDecimal refund = refundStrategy.refundAmount(booking.getTotalAmount(), show.getStartTime(), Instant.now());

        showSeatRepository.releaseSeatsForBooking(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);

        if (refund.signum() > 0) {
            String ref = paymentGateway.refund(bookingId, refund);
            paymentRepository.save(new Payment(bookingId, refund, PaymentStatus.REFUNDED, ref));
        }

        return new CancelResponse(bookingId, booking.getStatus().name(), refund);
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
