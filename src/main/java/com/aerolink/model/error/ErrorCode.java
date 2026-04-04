package com.aerolink.model.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Defines all application-level error codes for AeroLink.
 *
 * Format: AERO-{number}
 *   AERO-1xx → client/validation errors (maps to 4xx HTTP)
 *   AERO-2xx → upstream/integration errors (maps to 5xx HTTP)
 */
@Getter
public enum ErrorCode {

    ICAO_LIMIT_EXCEEDED("AERO-101", "Number of ICAO codes exceeds the maximum allowed limit", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED("AERO-102", "Too many requests. Rate limit of 60 requests per minute exceeded", HttpStatus.TOO_MANY_REQUESTS),

    UPSTREAM_API_ERROR("AERO-201", "Upstream aviation API returned an error", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String description;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String description, HttpStatus httpStatus) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }
}
