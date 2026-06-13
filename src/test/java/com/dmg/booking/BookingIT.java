package com.dmg.booking;

import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.BookingRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    void bookingSeatWithoutAValidHold_isConflict() {
        Long alice = aliceId();
        Long availableSeat = showSeatIds().get(2); // never held
        assertThatThrownBy(() ->
                bookingService.book(alice, "k3", new BookingRequest(999L, List.of(availableSeat))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void bookEndpoint_requiresAuth() throws Exception {
        String body = "{\"holdId\":1,\"showSeatIds\":[" + showSeatIds().get(3) + "]}";
        mvc.perform(post("/bookings").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }
}
