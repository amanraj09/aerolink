package com.aerolink.exception;

import com.aerolink.model.error.ErrorCode;
import lombok.Getter;

/**
 * Custom application exception for AeroLink.
 *
 * Carries an {@link ErrorCode} so the global handler knows exactly
 * which HTTP status and error code to return — no need to inspect
 * exception types or messages in the handler.
 */
@Getter
public class AeroLinkException extends RuntimeException {

    private final ErrorCode errorCode;

    public AeroLinkException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public AeroLinkException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
