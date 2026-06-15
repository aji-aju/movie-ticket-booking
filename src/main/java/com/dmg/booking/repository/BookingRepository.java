package com.dmg.booking.repository;

import com.dmg.booking.domain.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Idempotency lookup is scoped to the user so one client cannot read another's booking. */
    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Pessimistic row lock so concurrent cancels of the same booking serialize (no double refund). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
