package com.dmg.booking.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Time-tiered refund policy:
 *   >= 24h before show  -> 100%
 *   2h .. 24h before     -> 50%
 *   < 2h (or after start) -> 0%
 */
@Component
public class TieredRefundStrategy implements RefundStrategy {

    private static final BigDecimal HALF = new BigDecimal("0.50");

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid, Instant showStart, Instant cancelledAt) {
        long hoursBefore = Duration.between(cancelledAt, showStart).toHours();
        if (hoursBefore >= 24) {
            return amountPaid;
        }
        if (hoursBefore >= 2) {
            return amountPaid.multiply(HALF);
        }
        return BigDecimal.ZERO;
    }
}
