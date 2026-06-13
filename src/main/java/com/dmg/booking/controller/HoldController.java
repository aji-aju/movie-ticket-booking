package com.dmg.booking.controller;

import com.dmg.booking.config.SecurityUtils;
import com.dmg.booking.dto.HoldRequest;
import com.dmg.booking.dto.HoldResponse;
import com.dmg.booking.service.HoldService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holds")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    public HoldResponse createHold(@Valid @RequestBody HoldRequest request) {
        return holdService.createHold(SecurityUtils.currentUserId(), request);
    }
}
