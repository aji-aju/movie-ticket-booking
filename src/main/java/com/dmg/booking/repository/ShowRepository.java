package com.dmg.booking.repository;

import com.dmg.booking.domain.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Shows optionally filtered by city name (case-insensitive). All single-valued
     * associations are fetch-joined so the DTO mapping is N+1-free.
     */
    @Query("""
           select s from Show s
             join fetch s.movie
             join fetch s.screen sc
             join fetch sc.theater t
             join fetch t.city c
           order by s.startTime
           """)
    List<Show> findAllWithDetails();

    @Query("""
           select s from Show s
             join fetch s.movie
             join fetch s.screen sc
             join fetch sc.theater t
             join fetch t.city c
           where lower(c.name) = lower(:city)
           order by s.startTime
           """)
    List<Show> findByCityWithDetails(@Param("city") String city);
}
