package com.dmg.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        String status,
        BigDecimal totalAmount,
        List<Long> showSeatIds,
        Instant createdAt
) {
}
