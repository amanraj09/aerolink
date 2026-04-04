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

    // 4xx http status
    ICAO_CODES_REQUIRED("AERO-101", "At least one ICAO code is required", HttpStatus.BAD_REQUEST),
    ICAO_LIMIT_EXCEEDED("AERO-102", "Number of ICAO codes exceeds the maximum allowed limit", HttpStatus.BAD_REQUEST),
    ICAO_CODE_INVALID_FORMAT("AERO-103", "", HttpStatus.BAD_REQUEST),
    UPSTREAM_RATE_LIMIT_EXCEEDED("AERO-104", "Too many requests. Upstream aviation API applied Rate limit of 60 requests per minute exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // 5xx http status
    UPSTREAM_SERVER_ERROR("AERO-201", "Upstream service returned an error. Please try again later", HttpStatus.BAD_GATEWAY),
    UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR("AERO-202", "Upstream service failed to respond. Please try again later", HttpStatus.SERVICE_UNAVAILABLE),
    UPSTREAM_CLIENT_ERROR("AERO-203", "Upstream service rejected the request", HttpStatus.BAD_GATEWAY),
    UPSTREAM_RESPONSE_PARSE_ERROR("AERO-204", "Upstream service returned an unrecognized response", HttpStatus.BAD_GATEWAY),
    INTERNAL_SERVER_ERROR("AERO-205", "An unexpected error occurred. Please try again later", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String description;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String description, HttpStatus httpStatus) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }
}
