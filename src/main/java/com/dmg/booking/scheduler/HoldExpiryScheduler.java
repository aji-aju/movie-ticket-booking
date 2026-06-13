package com.dmg.booking.scheduler;

import com.dmg.booking.repository.SeatHoldRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Periodically releases expired seat holds so unconfirmed seats return to the pool
 * without blocking the booking flow. Interval is configurable
 * ({@code booking.scheduler.hold-sweep-ms}).
 */
@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    public HoldExpiryScheduler(ShowSeatRepository showSeatRepository,
                               SeatHoldRepository seatHoldRepository) {
        this.showSeatRepository = showSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
    }

    @Scheduled(fixedDelayString = "${booking.scheduler.hold-sweep-ms}")
    @Transactional
    public void sweepExpiredHolds() {
        Instant now = Instant.now();
        int releasedSeats = showSeatRepository.releaseExpiredHolds(now);
        int expiredHolds = seatHoldRepository.markExpired(now);
        if (releasedSeats > 0 || expiredHolds > 0) {
            log.info("Hold sweep released {} seats and expired {} hold records", releasedSeats, expiredHolds);
        }
    }
}
