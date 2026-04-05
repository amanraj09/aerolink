package com.aerolink.exception;

/**
 * Thrown when the upstream aviation API returns a transient 5xx response (502, 503) that is worth
 * retrying.
 */
public class UpstreamTransientServerException extends UpstreamServerException {

  public UpstreamTransientServerException(int httpStatus) {
    super(httpStatus);
  }
}
