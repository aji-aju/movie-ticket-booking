package com.dmg.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ShowSummaryDto(
        Long id,
        String movieTitle,
        String theaterName,
        String screenName,
        String city,
        Instant startTime,
        BigDecimal basePrice
) {
}
