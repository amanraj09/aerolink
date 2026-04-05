package com.aerolink.exception;

import com.aerolink.model.error.ErrorCode;
import com.aerolink.model.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AeroLinkException.class)
  public ResponseEntity<ErrorResponse> handleAeroLinkException(AeroLinkException ex) {
    log.error("AeroLinkException: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());

    var responseBuilder = ResponseEntity.status(ex.getErrorCode().getHttpStatus());

    if (ex.getRetryAfterSeconds() != null) {
      responseBuilder.header("Retry-After", ex.getRetryAfterSeconds().toString());
    }

    return responseBuilder.body(new ErrorResponse(ex.getErrorCode().getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex) {
    log.error("Missing request parameter: {}", ex.getParameterName());
    return ResponseEntity.status(ErrorCode.ICAO_CODES_REQUIRED.getHttpStatus())
        .body(
            new ErrorResponse(
                ErrorCode.ICAO_CODES_REQUIRED.getCode(),
                ErrorCode.ICAO_CODES_REQUIRED.getDescription()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(
            new ErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDescription()));
  }
}
