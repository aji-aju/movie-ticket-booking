package com.dmg.booking.controller;

import com.dmg.booking.config.SecurityUtils;
import com.dmg.booking.dto.BookingRequest;
import com.dmg.booking.dto.BookingResponse;
import com.dmg.booking.dto.CancelResponse;
import com.dmg.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "Confirm and view bookings (CUSTOMER)")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Confirm a booking",
            description = "Books the seats held by your hold (pessimistic lock + no double-allocation). "
                    + "Send an Idempotency-Key header to make a retried submit safe.")
    public BookingResponse book(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        return bookingService.book(SecurityUtils.currentUserId(), idempotencyKey, request);
    }

    @GetMapping
    @Operation(summary = "My bookings", description = "The authenticated customer's booking history (newest first).")
    public List<BookingResponse> myBookings() {
        return bookingService.history(SecurityUtils.currentUserId());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking",
            description = "Releases the seats back to the pool and refunds per the time-based policy "
                    + "(100% > 24h, 50% within a day, 0% last-minute).")
    public CancelResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(SecurityUtils.currentUserId(), id);
    }
}
