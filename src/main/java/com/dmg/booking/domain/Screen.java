package com.dmg.booking.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "screen")
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false)
    private String name;

    protected Screen() {
    }

    public Screen(Theater theater, String name) {
        this.theater = theater;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Theater getTheater() {
        return theater;
    }

    public String getName() {
        return name;
    }

    public void setTheater(Theater theater) {
        this.theater = theater;
    }

    public void setName(String name) {
        this.name = name;
    }
}
