package com.dmg.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    protected Show() {
    }

    public Show(Screen screen, Movie movie, Instant startTime, BigDecimal basePrice) {
        this.screen = screen;
        this.movie = movie;
        this.startTime = startTime;
        this.basePrice = basePrice;
    }

    public Long getId() {
        return id;
    }

    public Screen getScreen() {
        return screen;
    }

    public Movie getMovie() {
        return movie;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
