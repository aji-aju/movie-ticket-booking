package com.dmg.booking.repository;

import com.dmg.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Idempotency lookup is scoped to the user so one client cannot read another's booking. */
    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
