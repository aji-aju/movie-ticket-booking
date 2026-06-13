package com.dmg.booking.service;

import java.math.BigDecimal;

/** Abstraction over the payment provider so a real gateway can replace the mock. */
public interface PaymentGateway {

    /** Charge the amount and return a provider reference. Throws if the charge fails. */
    String charge(Long bookingId, BigDecimal amount);
}
