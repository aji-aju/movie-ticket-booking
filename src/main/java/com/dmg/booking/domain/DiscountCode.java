package com.dmg.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "discount_code")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    /** For PERCENTAGE this is a percent (e.g. 10 = 10%); for FLAT it is an absolute amount. */
    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private boolean active;

    protected DiscountCode() {
    }

    public DiscountCode(String code, DiscountType type, BigDecimal value, boolean active) {
        this.code = code;
        this.type = type;
        this.value = value;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public boolean isActive() {
        return active;
    }
}
