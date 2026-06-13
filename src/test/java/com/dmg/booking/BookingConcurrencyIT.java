package com.dmg.booking;

import com.dmg.booking.domain.SeatStatus;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.repository.AppUserRepository;
import com.dmg.booking.repository.BookingRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import com.dmg.booking.service.BookingService;
import com.dmg.booking.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The headline correctness proof: many users hammer the SAME seat at the same time,
 * and exactly one booking must win — no double-allocation. Serialization comes from
 * the pessimistic SELECT ... FOR UPDATE in BookingService.
 */
class BookingConcurrencyIT extends AbstractIntegrationTest {

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

    @Test
    void concurrentBookingsOfSameSeat_exactlyOneSucceeds() throws Exception {
        Long alice = appUserRepository.findByUsername("alice").orElseThrow().getId();
        Long seatId = showSeatRepository.findByShowIdWithSeat(1L).get(0).getId();
        HoldResponse hold = holdService.createHold(alice, new HoldRequest(1L, List.of(seatId)));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                                  // release all threads together
                try {
                    bookingService.book(alice, "key-" + idx,
                            new BookingRequest(hold.holdId(), List.of(seatId)));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();               // ConflictException for the losers
                }
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(success.get()).isEqualTo(1);
        assertThat(failure.get()).isEqualTo(threads - 1);
        assertThat(showSeatRepository.findById(seatId).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.BOOKED);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }
}
