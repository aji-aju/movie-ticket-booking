package com.dmg.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShowControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void browseShows_isPublic_andReturnsSeededShow() throws Exception {
        // Position-independent: enriched seed (V8) adds shows that may sort ahead of the
        // seeded Bengaluru/Inception show by start_time; it must still be present.
        mvc.perform(get("/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].movieTitle", hasItem("Inception")))
                .andExpect(jsonPath("$[*].city", hasItem("Bengaluru")));
    }

    @Test
    void browseShows_filterByCity() throws Exception {
        mvc.perform(get("/shows").param("city", "bengaluru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void browseSeats_returnsTenAvailableSeats() throws Exception {
        mvc.perform(get("/shows/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void unknownShowSeats_returns404() throws Exception {
        mvc.perform(get("/shows/9999/seats"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        // /admin/** requires ADMIN; an unauthenticated request is rejected by the filter chain.
        mvc.perform(get("/admin/ping"))
                .andExpect(status().isUnauthorized());
    }
}
