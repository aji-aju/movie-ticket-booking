package com.dmg.booking;

import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.CancelResponse;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.ConflictException;
import com.dmg.booking.exception.ForbiddenException;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelIT extends AbstractIntegrationTest {

    @Autowired
    HoldService holdService;
    @Autowired
    BookingService bookingService;
    @Autowired
    ShowSeatRepository showSeatRepository;
    @Autowired
    AppUserRepository appUserRepository;

    private Long aliceId() {
        return appUserRepository.findByUsername("alice").orElseThrow().getId();
    }

    private Long bobId() {
        return appUserRepository.findByUsername("bob").orElseThrow().getId();
    }

    private List<Long> seats() {
        return showSeatRepository.findByShowIdWithSeat(1L).stream().map(ShowSeat::getId).toList();
    }

    private BookingResponse bookSeat(Long user, String key, Long seat) {
        HoldResponse hold = holdService.createHold(user, new HoldRequest(1L, List.of(seat)));
        return bookingService.book(user, key, new BookingRequest(hold.holdId(), List.of(seat)));
    }

    @Test
    void cancel_releasesSeat_andMarksCancelled() {
        Long alice = aliceId();
        Long seat = seats().get(0);
        BookingResponse booking = bookSeat(alice, "cx1", seat);

        CancelResponse cancel = bookingService.cancel(alice, booking.bookingId());

        assertThat(cancel.status()).isEqualTo("CANCELLED");
        assertThat(cancel.refundAmount()).isNotNull();
        assertThat(showSeatRepository.findById(seat).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void cancel_freesSeatForRebooking() {
        Long alice = aliceId();
        Long seat = seats().get(1);
        BookingResponse booking = bookSeat(alice, "cx2", seat);
        bookingService.cancel(alice, booking.bookingId());

        // The freed seat can be held and booked again.
        BookingResponse rebook = bookSeat(alice, "cx2b", seat);
        assertThat(rebook.status()).isEqualTo("CONFIRMED");
        assertThat(showSeatRepository.findById(seat).orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void cannotCancelAnotherUsersBooking() {
        Long alice = aliceId();
        Long bob = bobId();
        BookingResponse booking = bookSeat(alice, "cx3", seats().get(2));

        assertThatThrownBy(() -> bookingService.cancel(bob, booking.bookingId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void doubleCancel_isConflict() {
        Long alice = aliceId();
        BookingResponse booking = bookSeat(alice, "cx4", seats().get(3));
        bookingService.cancel(alice, booking.bookingId());

        assertThatThrownBy(() -> bookingService.cancel(alice, booking.bookingId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelUnknownBooking_isNotFound() {
        assertThatThrownBy(() -> bookingService.cancel(aliceId(), 999_999L))
                .isInstanceOf(NotFoundException.class);
    }
}
