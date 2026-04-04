package com.aerolink.model.error;

/**
 * Standard error response returned to the client when a request fails.
 *
 * @param errorCode   AeroLink error code (e.g. "AERO-100") identifying the specific error
 * @param message     Human-readable description of what went wrong
 */
public record ErrorResponse(
        String errorCode,
        String message
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getDescription());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message);
    }
}
