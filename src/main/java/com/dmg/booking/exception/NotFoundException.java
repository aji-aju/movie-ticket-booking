package com.dmg.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Maps to HTTP 404. Replaced by richer error responses in the M9 polish pass. */
public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
