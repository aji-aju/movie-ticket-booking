package com.dmg.booking.service;

import com.dmg.booking.domain.SeatHold;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.SeatHoldRepository;
import com.dmg.booking.repository.ShowRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class HoldService {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final long ttlMinutes;

    public HoldService(SeatHoldRepository seatHoldRepository,
                       ShowSeatRepository showSeatRepository,
                       ShowRepository showRepository,
                       @Value("${booking.hold.ttl-minutes}") long ttlMinutes) {
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Hold the requested seats for the user. The conditional UPDATE only flips seats
     * that are still AVAILABLE; if fewer than requested flip, we throw 409 and the
     * whole transaction rolls back (no partial holds, no orphaned hold record).
     */
    @Transactional
    public HoldResponse createHold(Long userId, HoldRequest request) {
        // Validate the show up front so a bad showId is a clean 404, not a
        // foreign-key violation surfacing as a 500.
        if (!showRepository.existsById(request.showId())) {
            throw new NotFoundException("Show " + request.showId() + " not found");
        }

        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);

        SeatHold hold = seatHoldRepository.save(new SeatHold(request.showId(), userId, expiresAt));

        int held = showSeatRepository.holdSeats(
                request.showSeatIds(), request.showId(), hold.getId(), expiresAt);

        if (held != request.showSeatIds().size()) {
            throw new ConflictException("One or more selected seats are not available for this show");
        }
        return new HoldResponse(hold.getId(), expiresAt, request.showSeatIds());
    }
}
