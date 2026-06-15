package com.dmg.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HoldRequest(
        @NotNull @Positive Long showId,
        @NotEmpty @Size(max = 10, message = "at most 10 seats per hold") List<@Positive Long> showSeatIds
) {
}
