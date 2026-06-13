package com.dmg.booking;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests. Uses a single Postgres container shared across all
 * test classes (singleton pattern), and resets mutable booking state before every
 * test so the suite is order-independent (the seeded catalog stays intact).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20"); // headroom for concurrency tests
    }

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetMutableState() {
        // Return all seats to AVAILABLE and clear holds/bookings; keep the seeded catalog.
        jdbc.execute("UPDATE show_seat SET status='AVAILABLE', hold_id=NULL, held_until=NULL, booking_id=NULL");
        jdbc.execute("DELETE FROM payment");
        jdbc.execute("DELETE FROM seat_hold");
        jdbc.execute("DELETE FROM booking");
    }
}
