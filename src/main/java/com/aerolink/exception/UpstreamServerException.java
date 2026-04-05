package com.aerolink.exception;

import org.springframework.web.client.RestClientException;

/**
 * Thrown when the upstream aviation API returns a 5xx server error response (500, 502, 503, etc.).
 */
public class UpstreamServerException extends RestClientException {

  private final int httpStatus;

  public UpstreamServerException(int httpStatus) {
    super("Upstream server error: HTTP " + httpStatus);
    this.httpStatus = httpStatus;
  }

  public int getHttpStatus() {
    return httpStatus;
  }
}
