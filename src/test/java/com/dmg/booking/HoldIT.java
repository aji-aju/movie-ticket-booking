package com.dmg.booking;

import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HoldIT extends AbstractIntegrationTest {

    @Autowired
    HoldService holdService;
    @Autowired
    ShowSeatRepository showSeatRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    MockMvc mvc;

    private Long aliceId() {
        return appUserRepository.findByUsername("alice").orElseThrow().getId();
    }

    private List<Long> showSeatIds() {
        return showSeatRepository.findByShowIdWithSeat(1L).stream().map(ShowSeat::getId).toList();
    }

    @Test
    void holdAvailableSeats_marksThemHeld() {
        List<Long> toHold = List.of(showSeatIds().get(0), showSeatIds().get(1));
        HoldResponse resp = holdService.createHold(aliceId(), new HoldRequest(1L, toHold));

        assertThat(resp.holdId()).isNotNull();
        assertThat(resp.expiresAt()).isAfter(Instant.now());
        assertThat(showSeatRepository.findAllById(toHold))
                .allMatch(ss -> ss.getStatus() == SeatStatus.HELD);
    }

    @Test
    void holdingAnAlreadyHeldSeat_throwsConflict() {
        Long seat = showSeatIds().get(2);
        holdService.createHold(aliceId(), new HoldRequest(1L, List.of(seat)));

        assertThatThrownBy(() -> holdService.createHold(aliceId(), new HoldRequest(1L, List.of(seat))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void holdWithUnknownShow_isNotFound() {
        assertThatThrownBy(() ->
                holdService.createHold(aliceId(), new HoldRequest(999L, List.of(showSeatIds().get(0)))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Transactional
    void expiredHolds_areReleasedBackToAvailable() {
        Long seat = showSeatIds().get(3);
        holdService.createHold(aliceId(), new HoldRequest(1L, List.of(seat)));

        // Simulate the sweeper firing after the TTL by passing a far-future cutoff.
        int released = showSeatRepository.releaseExpiredHolds(Instant.now().plus(1, ChronoUnit.DAYS));

        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(showSeatRepository.findById(seat).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void holdEndpoint_requiresCustomerAuth() throws Exception {
        String body = "{\"showId\":1,\"showSeatIds\":[" + showSeatIds().get(4) + "]}";

        mvc.perform(post("/holds").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/holds").with(httpBasic("alice", "password"))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void holdWithEmptySeatList_isBadRequest() throws Exception {
        mvc.perform(post("/holds").with(httpBasic("alice", "password"))
                        .contentType("application/json").content("{\"showId\":1,\"showSeatIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
