package com.dmg.booking.repository;

import com.dmg.booking.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBookingId(Long bookingId);
}
