package com.dmg.booking.strategy;

import com.dmg.booking.domain.DiscountCode;
import com.dmg.booking.domain.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getType() {
        return DiscountType.PERCENTAGE;
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal, DiscountCode code) {
        BigDecimal discount = subtotal
                .multiply(code.getValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return subtotal.subtract(discount);
    }
}
