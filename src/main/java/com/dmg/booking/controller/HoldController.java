package com.dmg.booking.controller;

import com.dmg.booking.config.SecurityUtils;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.service.HoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holds")
@Tag(name = "Holds", description = "Time-bound seat holds (CUSTOMER)")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold seats",
            description = "Atomically holds AVAILABLE seats for the configured TTL. Returns 409 if any seat is "
                    + "no longer available, 404 if the show does not exist.")
    public HoldResponse createHold(@Valid @RequestBody HoldRequest request) {
        return holdService.createHold(SecurityUtils.currentUserId(), request);
    }
}
