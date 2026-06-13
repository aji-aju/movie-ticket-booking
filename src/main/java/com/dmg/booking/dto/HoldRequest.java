package com.dmg.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HoldRequest(
        @NotNull Long showId,
        @NotEmpty List<Long> showSeatIds
) {
}
