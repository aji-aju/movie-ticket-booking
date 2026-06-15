package com.dmg.booking.event;

import java.math.BigDecimal;

/** Published when a booking is confirmed; handled asynchronously after the tx commits. */
public record BookingConfirmedEvent(Long bookingId, Long userId, BigDecimal totalAmount) {
}
