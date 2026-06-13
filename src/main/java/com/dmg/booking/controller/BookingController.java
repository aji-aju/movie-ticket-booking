package com.dmg.booking.controller;

import com.dmg.booking.config.SecurityUtils;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse book(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        return bookingService.book(SecurityUtils.currentUserId(), idempotencyKey, request);
    }

    @GetMapping
    public List<BookingResponse> myBookings() {
        return bookingService.history(SecurityUtils.currentUserId());
    }
}
