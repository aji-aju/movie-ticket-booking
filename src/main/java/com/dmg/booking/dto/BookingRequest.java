package com.dmg.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookingRequest(
        @NotNull Long holdId,
        @NotEmpty List<Long> showSeatIds,
        String discountCode
) {
    /** Convenience for callers/tests without a discount code. */
    public BookingRequest(Long holdId, List<Long> showSeatIds) {
        this(holdId, showSeatIds, null);
    }
}
