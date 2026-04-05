package com.aerolink.metrics.aspect;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingServletRequestParameterException;

@Slf4j
@Aspect
@Component
public class ExceptionMetricsAspect {

  private final AeroLinkMetrics metrics;

  public ExceptionMetricsAspect(AeroLinkMetrics metrics) {
    this.metrics = metrics;
  }

  @AfterThrowing(
      pointcut =
          "within(com.aerolink..*) && !within(com.aerolink.properties..*) && !within(com.aerolink.model..*)",
      throwing = "ex")
  public void recordExceptionMetrics(Exception ex) {
    if (ex instanceof AeroLinkException alex) {
      metrics.recordError(alex.getErrorCode());
    } else if (ex instanceof MissingServletRequestParameterException) {
      metrics.recordError(ErrorCode.ICAO_CODES_REQUIRED);
    } else {
      metrics.recordError(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
