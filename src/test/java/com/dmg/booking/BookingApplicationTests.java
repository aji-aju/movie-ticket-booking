package com.dmg.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: boots the full Spring context against a real Postgres (Testcontainers).
 * This verifies in one shot that:
 *  - Flyway migrations V1 (schema) + V2 (seed) apply cleanly, and
 *  - Hibernate's ddl-auto=validate confirms every JPA entity matches the Flyway schema.
 */
@SpringBootTest
@Testcontainers
class BookingApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
        // Context startup is the assertion: if Flyway or schema-validation fails, this test fails.
    }
}
