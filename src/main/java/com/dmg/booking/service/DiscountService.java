package com.dmg.booking.service;

import com.dmg.booking.domain.DiscountCode;
import com.dmg.booking.domain.DiscountType;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.DiscountCodeRepository;
import com.dmg.booking.strategy.DiscountStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiscountService {

    private final DiscountCodeRepository discountCodeRepository;
    private final Map<DiscountType, DiscountStrategy> strategies;

    // OCP registry: a new DiscountStrategy @Component wires itself in by its type, no edits here.
    public DiscountService(DiscountCodeRepository discountCodeRepository, List<DiscountStrategy> strategyList) {
        this.discountCodeRepository = discountCodeRepository;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(DiscountStrategy::getType, Function.identity()));
    }

    /** Applies the (active) discount code to the subtotal; null/blank -> unchanged; invalid -> 404. */
    public BigDecimal applyDiscount(BigDecimal subtotal, String code) {
        if (code == null || code.isBlank()) {
            return subtotal;
        }
        DiscountCode discount = discountCodeRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new NotFoundException("Discount code '" + code + "' is invalid or inactive"));
        DiscountStrategy strategy = strategies.get(discount.getType());
        if (strategy == null) {
            throw new IllegalStateException("No discount strategy for type " + discount.getType());
        }
        return strategy.apply(subtotal, discount).max(BigDecimal.ZERO);  // never below zero
    }
}
