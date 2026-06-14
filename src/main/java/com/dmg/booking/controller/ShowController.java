package com.dmg.booking.controller;

import com.dmg.booking.dto.ShowSeatDto;
import com.dmg.booking.dto.ShowSummaryDto;
import com.dmg.booking.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shows")
@Tag(name = "Shows", description = "Public catalog — browse shows and seat availability")
public class ShowController {

    private final CatalogService catalogService;

    public ShowController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "List shows",
            description = "All shows, optionally filtered by city (case-insensitive). Public.")
    public List<ShowSummaryDto> listShows(@RequestParam(required = false) String city) {
        return catalogService.listShows(city);
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "List seats for a show",
            description = "Seats with status (AVAILABLE / HELD / BOOKED) and price. Public.")
    public List<ShowSeatDto> showSeats(@PathVariable Long id) {
        return catalogService.getShowSeats(id);
    }
}
