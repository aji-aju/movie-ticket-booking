package com.dmg.booking.repository;

import com.dmg.booking.domain.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    /** Mark ACTIVE holds past their expiry as EXPIRED (run by the sweeper). */
    @Modifying(clearAutomatically = true)
    @Query("""
           update SeatHold h
              set h.status = com.dmg.booking.domain.HoldStatus.EXPIRED
            where h.status = com.dmg.booking.domain.HoldStatus.ACTIVE
              and h.expiresAt < :cutoff
           """)
    int markExpired(@Param("cutoff") Instant cutoff);
}
