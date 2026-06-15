package com.dmg.booking.dto;

import java.math.BigDecimal;

public record CancelResponse(
        Long bookingId,
        String status,
        BigDecimal refundAmount
) {
}
