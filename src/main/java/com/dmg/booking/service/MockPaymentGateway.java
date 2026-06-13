package com.dmg.booking.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Stub gateway that always succeeds. Swap for a real provider behind {@link PaymentGateway}. */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String charge(Long bookingId, BigDecimal amount) {
        return "MOCK-" + UUID.randomUUID();
    }
}
