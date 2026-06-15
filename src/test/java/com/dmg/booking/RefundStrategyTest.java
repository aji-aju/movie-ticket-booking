package com.dmg.booking;

import com.dmg.booking.strategy.RefundStrategy;
import com.dmg.booking.strategy.TieredRefundStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RefundStrategyTest {

    private final RefundStrategy refund = new TieredRefundStrategy();
    private final Instant show = Instant.parse("2026-06-20T18:00:00Z");
    private final BigDecimal paid = new BigDecimal("400");

    @Test
    void fullRefund_whenWellBeforeShow() {
        assertThat(refund.refundAmount(paid, show, show.minus(48, ChronoUnit.HOURS)))
                .isEqualByComparingTo("400");
    }

    @Test
    void halfRefund_withinTheDay() {
        assertThat(refund.refundAmount(paid, show, show.minus(5, ChronoUnit.HOURS)))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void noRefund_lastMinute() {
        assertThat(refund.refundAmount(paid, show, show.minus(1, ChronoUnit.HOURS)))
                .isEqualByComparingTo("0");
    }
}
