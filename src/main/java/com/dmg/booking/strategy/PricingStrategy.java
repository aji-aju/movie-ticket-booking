package com.dmg.booking.strategy;

import com.dmg.booking.domain.Show;
import com.dmg.booking.domain.ShowSeat;

import java.math.BigDecimal;

/** Computes the effective price of a seat for a show (tier base +/- situational pricing). */
public interface PricingStrategy {

    BigDecimal priceFor(ShowSeat seat, Show show);
}
