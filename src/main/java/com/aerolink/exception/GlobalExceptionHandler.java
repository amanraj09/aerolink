package com.aerolink.exception;

import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.error.ErrorCode;
import com.aerolink.model.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all AeroLink exceptions.
 *
 * <p>Centralises error handling in one place — controllers and services simply throw {@link
 * AeroLinkException} and this handler converts it into a consistent {@link ErrorResponse} with the
 * correct HTTP status.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final AeroLinkMetrics metrics;

  public GlobalExceptionHandler(AeroLinkMetrics metrics) {
    this.metrics = metrics;
  }

  @ExceptionHandler(AeroLinkException.class)
  public ResponseEntity<ErrorResponse> handleAeroLinkException(AeroLinkException ex) {
    log.error("AeroLinkException: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());
    metrics.recordError(ex.getErrorCode());

    var responseBuilder = ResponseEntity.status(ex.getErrorCode().getHttpStatus());

    // Add Retry-After header if available (for 429 rate limit responses)
    if (ex.getRetryAfterSeconds() != null) {
      responseBuilder.header("Retry-After", ex.getRetryAfterSeconds().toString());
    }

    return responseBuilder.body(new ErrorResponse(ex.getErrorCode().getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex) {
    log.error("Missing request parameter: {}", ex.getParameterName());
    metrics.recordError(ErrorCode.ICAO_CODES_REQUIRED);
    return ResponseEntity.status(ErrorCode.ICAO_CODES_REQUIRED.getHttpStatus())
        .body(
            new ErrorResponse(
                ErrorCode.ICAO_CODES_REQUIRED.getCode(),
                ErrorCode.ICAO_CODES_REQUIRED.getDescription()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    metrics.recordError(ErrorCode.INTERNAL_SERVER_ERROR);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(
            new ErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDescription()));
  }
}
