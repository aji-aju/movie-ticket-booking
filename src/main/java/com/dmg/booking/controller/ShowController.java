package com.dmg.booking.controller;

import com.dmg.booking.dto.ShowSeatDto;
import com.dmg.booking.dto.ShowSummaryDto;
import com.dmg.booking.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final CatalogService catalogService;

    public ShowController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ShowSummaryDto> listShows(@RequestParam(required = false) String city) {
        return catalogService.listShows(city);
    }

    @GetMapping("/{id}/seats")
    public List<ShowSeatDto> showSeats(@PathVariable Long id) {
        return catalogService.getShowSeats(id);
    }
}
