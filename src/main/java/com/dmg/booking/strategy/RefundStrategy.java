package com.dmg.booking.strategy;

import java.math.BigDecimal;
import java.time.Instant;

/** Computes the refund due when a booking is cancelled (used by the cancel flow in M7). */
public interface RefundStrategy {

    BigDecimal refundAmount(BigDecimal amountPaid, Instant showStart, Instant cancelledAt);
}
