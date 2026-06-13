package com.dmg.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Maps to HTTP 409 — used when a seat is no longer available to hold/book. */
public class ConflictException extends ResponseStatusException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
