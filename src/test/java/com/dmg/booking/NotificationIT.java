package com.dmg.booking;

import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.NotificationRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class NotificationIT extends AbstractIntegrationTest {

    @Autowired
    HoldService holdService;
    @Autowired
    BookingService bookingService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    ShowSeatRepository showSeatRepository;

    private Long aliceId() {
        return appUserRepository.findByUsername("alice").orElseThrow().getId();
    }

    private List<Long> seats() {
        return showSeatRepository.findByShowIdWithSeat(1L).stream().map(ShowSeat::getId).toList();
    }

    @Test
    void confirmation_isSentAsynchronouslyAfterBooking() {
        Long alice = aliceId();
        Long seat = seats().get(0);
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(seat)));
        BookingResponse booking = bookingService.book(alice, "notif-1", new BookingRequest(hold.holdId(), List.of(seat)));

        // Async + AFTER_COMMIT: the notification appears shortly after, off the booking thread.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notificationRepository.findByBookingId(booking.bookingId())).hasSize(1));
    }
}
