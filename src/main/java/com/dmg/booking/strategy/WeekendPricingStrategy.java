package com.dmg.booking.strategy;

import com.dmg.booking.domain.Show;
import com.dmg.booking.domain.ShowSeat;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.ZoneOffset;

/**
 * Tier price (stored per seat: REGULAR/PREMIUM) with a weekend surcharge applied
 * when the show falls on Saturday/Sunday (UTC).
 */
@Component
public class WeekendPricingStrategy implements PricingStrategy {

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.25");

    @Override
    public BigDecimal priceFor(ShowSeat seat, Show show) {
        BigDecimal base = seat.getPrice();
        DayOfWeek day = show.getStartTime().atZone(ZoneOffset.UTC).getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        BigDecimal price = weekend ? base.multiply(WEEKEND_MULTIPLIER) : base;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
