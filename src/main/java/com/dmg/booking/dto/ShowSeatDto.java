package com.dmg.booking.dto;

import java.math.BigDecimal;

public record ShowSeatDto(
        Long id,
        String rowLabel,
        int seatNumber,
        String tier,
        String status,
        BigDecimal price
) {
}
