package com.dmg.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-HTTP integration test: random Tomcat port + TestRestTemplate exercising the
 * full servlet + Spring Security stack against a real Postgres. MockMvc missed
 * live-only behavior (PathPattern base-path matching; JDBC null-param typing in
 * lower(:city)), so these guard the exact paths that broke when first run for real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealServerIT {

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AbstractIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AbstractIntegrationTest.POSTGRES::getPassword);
    }

    @Autowired
    TestRestTemplate rest;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.execute("UPDATE show_seat SET status='AVAILABLE', hold_id=NULL, held_until=NULL, booking_id=NULL");
        jdbc.execute("DELETE FROM notification");
        jdbc.execute("DELETE FROM payment");
        jdbc.execute("DELETE FROM seat_hold");
        jdbc.execute("DELETE FROM booking");
    }

    private static HttpEntity<String> holdBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>("{\"showId\":1,\"showSeatIds\":[1]}", headers);
    }

    @Test
    void getShows_isPublic_andReturnsData() {
        ResponseEntity<String> r = rest.getForEntity("/shows", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);   // was 401 (matcher) / 500 (lower(null)) live
        assertThat(r.getBody()).contains("Inception").contains("Bengaluru");
    }

    @Test
    void getShows_filteredByCity_isPublic() {
        ResponseEntity<String> r = rest.getForEntity("/shows?city=bengaluru", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Inception");
    }

    @Test
    void getShowSeats_isPublic() {
        ResponseEntity<String> r = rest.getForEntity("/shows/1/seats", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void postHolds_withoutAuth_is401() {
        ResponseEntity<String> r = rest.exchange("/holds", HttpMethod.POST, holdBody(), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postHolds_asAdmin_is403_customerOnly() {
        ResponseEntity<String> r = rest.withBasicAuth("admin", "password")
                .exchange("/holds", HttpMethod.POST, holdBody(), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);   // RBAC: admin must not hold
    }

    @Test
    void postHolds_asCustomer_succeeds() {
        ResponseEntity<String> r = rest.withBasicAuth("alice", "password")
                .exchange("/holds", HttpMethod.POST, holdBody(), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).contains("holdId");
    }

    @Test
    void apiDocs_isPublic() {
        ResponseEntity<String> r = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Movie Ticket Booking API");
    }

    @Test
    void postHolds_unknownShow_is404_notServerError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> body = new HttpEntity<>("{\"showId\":999,\"showSeatIds\":[1]}", headers);
        ResponseEntity<String> r = rest.withBasicAuth("alice", "password")
                .exchange("/holds", HttpMethod.POST, body, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);   // was 500 (FK violation)
    }

    @Test
    void cannotBookAnotherUsersHold_idor() {
        // Alice holds seat 1
        ResponseEntity<String> holdResp = rest.withBasicAuth("alice", "password")
                .exchange("/holds", HttpMethod.POST, holdBody(), String.class);
        assertThat(holdResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long holdId = extractHoldId(holdResp.getBody());

        // Bob tries to book Alice's hold -> must be 403 (not 200, not 500)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> bookBody = new HttpEntity<>("{\"holdId\":" + holdId + ",\"showSeatIds\":[1]}", headers);
        ResponseEntity<String> r = rest.withBasicAuth("bob", "password")
                .exchange("/bookings", HttpMethod.POST, bookBody, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static long extractHoldId(String json) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"holdId\":(\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("no holdId in: " + json);
        }
        return Long.parseLong(m.group(1));
    }
}
