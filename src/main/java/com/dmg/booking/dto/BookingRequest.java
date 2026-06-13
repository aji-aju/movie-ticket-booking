package com.dmg.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookingRequest(
        @NotNull Long holdId,
        @NotEmpty List<Long> showSeatIds
) {
}
