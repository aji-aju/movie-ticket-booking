package com.dmg.booking.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    protected Movie() {
    }

    public Movie(String title, int durationMin) {
        this.title = title;
        this.durationMin = durationMin;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMin() {
        return durationMin;
    }
}
