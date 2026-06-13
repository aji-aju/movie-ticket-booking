package com.dmg.booking.repository;

import com.dmg.booking.domain.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    @Query("""
           select ss from ShowSeat ss
             join fetch ss.seat seat
           where ss.show.id = :showId
           order by seat.rowLabel, seat.seatNumber
           """)
    List<ShowSeat> findByShowIdWithSeat(@Param("showId") Long showId);
}
