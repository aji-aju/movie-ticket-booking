package com.dmg.booking;

import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountBookingIT extends AbstractIntegrationTest {

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

    private List<Long> seats() {
        return showSeatRepository.findByShowIdWithSeat(1L).stream().map(ShowSeat::getId).toList();
    }

    private BigDecimal book(Long user, String key, Long seat, String discountCode) {
        HoldResponse hold = holdService.createHold(user, new HoldRequest(1L, List.of(seat)));
        BookingResponse resp = bookingService.book(user, key, new BookingRequest(hold.holdId(), List.of(seat), discountCode));
        return resp.totalAmount();
    }

    @Test
    void percentageDiscount_takesTenPercentOff() {
        Long alice = aliceId();
        BigDecimal full = book(alice, "pct-full", seats().get(0), null);
        BigDecimal discounted = book(alice, "pct-save10", seats().get(1), "SAVE10");
        // seats 0 and 1 are the same tier (REGULAR) -> same effective price
        assertThat(discounted).isEqualByComparingTo(full.multiply(new BigDecimal("0.90")));
    }

    @Test
    void flatDiscount_subtractsFixedAmount() {
        Long alice = aliceId();
        BigDecimal full = book(alice, "flat-full", seats().get(2), null);
        BigDecimal discounted = book(alice, "flat-50", seats().get(3), "FLAT50");
        assertThat(discounted).isEqualByComparingTo(full.subtract(new BigDecimal("50")));
    }

    @Test
    void invalidOrInactiveCode_isNotFound() {
        Long alice = aliceId();
        Long seat = seats().get(4);
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(seat)));
        // EXPIRED is seeded but inactive; NOPE does not exist
        assertThatThrownBy(() -> bookingService.book(alice, "x1",
                new BookingRequest(hold.holdId(), List.of(seat), "EXPIRED")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.book(alice, "x2",
                new BookingRequest(hold.holdId(), List.of(seat), "NOPE")))
                .isInstanceOf(NotFoundException.class);
    }
}
