package com.dmg.booking.strategy;

import com.dmg.booking.domain.DiscountCode;
import com.dmg.booking.domain.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FlatDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getType() {
        return DiscountType.FLAT;
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal, DiscountCode code) {
        return subtotal.subtract(code.getValue());
    }
}
