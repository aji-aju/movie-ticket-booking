package com.dmg.booking;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: boots the full context against a real Postgres, which verifies
 * Flyway migrations apply and Hibernate validates the entities against the schema.
 */
class BookingApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
