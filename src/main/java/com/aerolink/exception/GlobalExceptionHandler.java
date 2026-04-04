package com.aerolink.exception;

import com.aerolink.model.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all AeroLink exceptions.
 *
 * Centralises error handling in one place — controllers and services
 * simply throw {@link AeroLinkException} and this handler converts it
 * into a consistent {@link ErrorResponse} with the correct HTTP status.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AeroLinkException.class)
    public ResponseEntity<ErrorResponse> handleAeroLinkException(AeroLinkException ex) {
        log.error("AeroLinkException: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(new ErrorResponse(ex.getErrorCode().getCode(), ex.getMessage()));
    }
}
