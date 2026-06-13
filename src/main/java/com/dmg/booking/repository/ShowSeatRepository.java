package com.dmg.booking.repository;

import com.dmg.booking.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    @Query("""
           select ss from ShowSeat ss
             join fetch ss.seat seat
           where ss.show.id = :showId
           order by seat.rowLabel, seat.seatNumber
           """)
    List<ShowSeat> findByShowIdWithSeat(@Param("showId") Long showId);

    /**
     * Atomically move the given seats from AVAILABLE to HELD — the lock-free
     * conditional-UPDATE "compare-and-swap". Returns the number of rows actually
     * transitioned; if that is less than requested, some seats were not available
     * and the caller must treat the hold as a conflict (and roll back).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
           update ShowSeat ss
              set ss.status = com.dmg.booking.domain.SeatStatus.HELD,
                  ss.holdId = :holdId,
                  ss.heldUntil = :until
            where ss.id in :ids
              and ss.show.id = :showId
              and ss.status = com.dmg.booking.domain.SeatStatus.AVAILABLE
           """)
    int holdSeats(@Param("ids") List<Long> ids,
                  @Param("showId") Long showId,
                  @Param("holdId") Long holdId,
                  @Param("until") Instant until);

    /** Release HELD seats whose hold has expired (held_until < cutoff) back to AVAILABLE. */
    @Modifying(clearAutomatically = true)
    @Query("""
           update ShowSeat ss
              set ss.status = com.dmg.booking.domain.SeatStatus.AVAILABLE,
                  ss.holdId = null,
                  ss.heldUntil = null
            where ss.status = com.dmg.booking.domain.SeatStatus.HELD
              and ss.heldUntil < :cutoff
           """)
    int releaseExpiredHolds(@Param("cutoff") Instant cutoff);

    /**
     * Pessimistic write-lock the given seats (SELECT ... FOR UPDATE), ordered by id
     * for a consistent lock order. Concurrent bookings of the same seat serialize
     * here, which is what prevents double-allocation at booking time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id in :ids order by ss.id")
    List<ShowSeat> lockSeatsForUpdate(@Param("ids") List<Long> ids);

    List<ShowSeat> findByBookingId(Long bookingId);
}
