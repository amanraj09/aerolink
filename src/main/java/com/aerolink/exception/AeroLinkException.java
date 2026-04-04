package com.aerolink.exception;

import com.aerolink.model.error.ErrorCode;
import lombok.Getter;

/**
 * Custom application exception for AeroLink.
 *
 * Carries an {@link ErrorCode} so the global handler knows exactly
 * which HTTP status and error code to return — no need to inspect
 * exception types or messages in the handler.
 *
 * Optionally carries {@code retryAfterSeconds} for rate-limit (429) responses.
 */
@Getter
public class AeroLinkException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Long retryAfterSeconds;

    public AeroLinkException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.retryAfterSeconds = null;
    }

    public AeroLinkException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.retryAfterSeconds = null;
    }

    public AeroLinkException(ErrorCode errorCode, Long retryAfterSeconds) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
