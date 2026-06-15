package com.dmg.booking;

import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.ForbiddenException;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.BookingRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingIT extends AbstractIntegrationTest {

    @Autowired
    HoldService holdService;
    @Autowired
    BookingService bookingService;
    @Autowired
    ShowSeatRepository showSeatRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    MockMvc mvc;

    private Long aliceId() {
        return appUserRepository.findByUsername("alice").orElseThrow().getId();
    }

    private Long bobId() {
        return appUserRepository.findByUsername("bob").orElseThrow().getId();
    }

    private List<Long> showSeatIds() {
        return showSeatRepository.findByShowIdWithSeat(1L).stream().map(ShowSeat::getId).toList();
    }

    @Test
    void bookHeldSeats_confirmsAndMarksBooked() {
        Long alice = aliceId();
        Long seat = showSeatIds().get(0);
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(seat)));

        BookingResponse resp = bookingService.book(alice, "k1", new BookingRequest(hold.holdId(), List.of(seat)));

        assertThat(resp.status()).isEqualTo("CONFIRMED");
        assertThat(resp.totalAmount()).isNotNull();
        assertThat(showSeatRepository.findById(seat).orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void idempotentReplay_returnsSameBooking() {
        Long alice = aliceId();
        Long seat = showSeatIds().get(1);
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(seat)));

        BookingResponse first = bookingService.book(alice, "samekey", new BookingRequest(hold.holdId(), List.of(seat)));
        BookingResponse second = bookingService.book(alice, "samekey", new BookingRequest(hold.holdId(), List.of(seat)));

        assertThat(second.bookingId()).isEqualTo(first.bookingId());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void bookingSeatNotCoveredByHold_isConflict() {
        Long alice = aliceId();
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(showSeatIds().get(2))));
        Long otherSeat = showSeatIds().get(3); // not part of the hold

        assertThatThrownBy(() ->
                bookingService.book(alice, "k3", new BookingRequest(hold.holdId(), List.of(otherSeat))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotBookAnotherUsersHold_idor() {
        Long alice = aliceId();
        Long bob = bobId();
        Long seat = showSeatIds().get(4);
        HoldResponse aliceHold = holdService.createHold(alice, new HoldRequest(1L, List.of(seat)));

        assertThatThrownBy(() ->
                bookingService.book(bob, "idor-key", new BookingRequest(aliceHold.holdId(), List.of(seat))))
                .isInstanceOf(ForbiddenException.class);

        // Alice's seat must remain held, not booked by Bob.
        assertThat(showSeatRepository.findById(seat).orElseThrow().getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void idempotencyKey_isScopedPerUser() {
        Long alice = aliceId();
        Long bob = bobId();
        Long seatA = showSeatIds().get(5);
        HoldResponse aliceHold = holdService.createHold(alice, new HoldRequest(1L, List.of(seatA)));
        bookingService.book(alice, "shared-key", new BookingRequest(aliceHold.holdId(), List.of(seatA)));

        Long seatB = showSeatIds().get(6);
        HoldResponse bobHold = holdService.createHold(bob, new HoldRequest(1L, List.of(seatB)));

        // Bob reuses Alice's key: he must NOT receive Alice's booking; the global-unique key collides.
        assertThatThrownBy(() ->
                bookingService.book(bob, "shared-key", new BookingRequest(bobHold.holdId(), List.of(seatB))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bookEndpoint_requiresAuth() throws Exception {
        String body = "{\"holdId\":1,\"showSeatIds\":[" + showSeatIds().get(7) + "]}";
        mvc.perform(post("/bookings").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }
}
