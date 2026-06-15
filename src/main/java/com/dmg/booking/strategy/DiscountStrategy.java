package com.dmg.booking.strategy;

import com.dmg.booking.domain.DiscountCode;
import com.dmg.booking.domain.DiscountType;

import java.math.BigDecimal;

/** One discount calculation per {@link DiscountType}; selected via a registry (OCP). */
public interface DiscountStrategy {

    DiscountType getType();

    BigDecimal apply(BigDecimal subtotal, DiscountCode code);
}
