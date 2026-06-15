package com.dmg.booking;

import com.dmg.booking.domain.Show;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.strategy.PricingStrategy;
import com.dmg.booking.strategy.WeekendPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingStrategyTest {

    private final PricingStrategy pricing = new WeekendPricingStrategy();

    @Test
    void weekday_chargesTierBase() {
        ShowSeat seat = mock(ShowSeat.class);
        when(seat.getPrice()).thenReturn(new BigDecimal("200"));
        Show show = mock(Show.class);
        when(show.getStartTime()).thenReturn(Instant.parse("2024-01-03T10:00:00Z")); // Wednesday

        assertThat(pricing.priceFor(seat, show)).isEqualByComparingTo("200");
    }

    @Test
    void weekend_appliesSurcharge() {
        ShowSeat seat = mock(ShowSeat.class);
        when(seat.getPrice()).thenReturn(new BigDecimal("200"));
        Show show = mock(Show.class);
        when(show.getStartTime()).thenReturn(Instant.parse("2024-01-06T10:00:00Z")); // Saturday

        assertThat(pricing.priceFor(seat, show)).isEqualByComparingTo("250.00"); // 200 * 1.25
    }
}
