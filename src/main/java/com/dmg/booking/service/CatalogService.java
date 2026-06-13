package com.dmg.booking.service;

import com.dmg.booking.domain.Show;
import com.dmg.booking.domain.ShowSeat;
import com.dmg.booking.dto.ShowSeatDto;
import com.dmg.booking.dto.ShowSummaryDto;
import com.dmg.booking.exception.NotFoundException;
import com.dmg.booking.repository.ShowRepository;
import com.dmg.booking.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    public CatalogService(ShowRepository showRepository, ShowSeatRepository showSeatRepository) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
    }

    @Transactional(readOnly = true)
    public List<ShowSummaryDto> listShows(String city) {
        List<Show> shows = (city == null || city.isBlank())
                ? showRepository.findAllWithDetails()
                : showRepository.findByCityWithDetails(city);
        return shows.stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowSeatDto> getShowSeats(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new NotFoundException("Show " + showId + " not found");
        }
        return showSeatRepository.findByShowIdWithSeat(showId).stream()
                .map(this::toSeatDto)
                .toList();
    }

    private ShowSummaryDto toSummary(Show s) {
        return new ShowSummaryDto(
                s.getId(),
                s.getMovie().getTitle(),
                s.getScreen().getTheater().getName(),
                s.getScreen().getName(),
                s.getScreen().getTheater().getCity().getName(),
                s.getStartTime(),
                s.getBasePrice());
    }

    private ShowSeatDto toSeatDto(ShowSeat ss) {
        return new ShowSeatDto(
                ss.getId(),
                ss.getSeat().getRowLabel(),
                ss.getSeat().getSeatNumber(),
                ss.getSeat().getTier().name(),
                ss.getStatus().name(),
                ss.getPrice());
    }
}
