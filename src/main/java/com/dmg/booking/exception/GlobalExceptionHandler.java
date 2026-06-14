package com.dmg.booking.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Safety net for low-level persistence errors. A constraint violation that reaches
 * the web layer is a client/data conflict, not a server bug, so it is mapped to 409
 * instead of leaking a 500 + stack trace. Domain errors (ConflictException -> 409,
 * NotFoundException -> 404) already extend ResponseStatusException and are mapped by
 * Spring; with problemdetails enabled they all render as RFC-7807 responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Conflict");
        problem.setDetail("The request could not be completed due to a data conflict.");
        return problem;
    }
}
