package com.dmg.booking.dto;

import java.time.Instant;
import java.util.List;

public record HoldResponse(
        Long holdId,
        Instant expiresAt,
        List<Long> heldShowSeatIds
) {
}
