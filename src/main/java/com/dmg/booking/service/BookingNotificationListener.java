package com.dmg.booking.service;

import com.dmg.booking.domain.Notification;
import com.dmg.booking.domain.NotificationChannel;
import com.dmg.booking.event.BookingConfirmedEvent;
import com.dmg.booking.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the booking confirmation notification asynchronously and only AFTER the booking
 * transaction commits — so it never blocks the booking response and never fires for a
 * rolled-back booking. Failures here must not affect the booking, hence the try/catch.
 */
@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final NotificationRepository notificationRepository;

    public BookingNotificationListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            String message = "Booking #" + event.bookingId()
                    + " is confirmed. Amount charged: " + event.totalAmount() + ".";
            notificationRepository.save(
                    new Notification(event.userId(), event.bookingId(), NotificationChannel.EMAIL, message));
            log.info("Sent confirmation notification for booking {} (user {})",
                    event.bookingId(), event.userId());
        } catch (Exception e) {
            log.warn("Failed to record confirmation notification for booking {}: {}",
                    event.bookingId(), e.getMessage());
        }
    }
}
