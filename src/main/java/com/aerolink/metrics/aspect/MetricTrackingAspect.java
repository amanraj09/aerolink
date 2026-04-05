package com.aerolink.metrics.aspect;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.metrics.annotation.TrackMetrics;
import com.aerolink.model.error.ErrorCode;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class MetricTrackingAspect {

  private final AeroLinkMetrics metrics;

  public MetricTrackingAspect(AeroLinkMetrics metrics) {
    this.metrics = metrics;
  }

  @Around("@annotation(trackMetrics)")
  public Object trackMetrics(ProceedingJoinPoint joinPoint, TrackMetrics trackMetrics)
      throws Throwable {
    // Record ICAO codes count if the first argument is a List of strings
    Object[] args = joinPoint.getArgs();
    if (args.length > 0 && args[0] instanceof Collection<?> collection) {
      metrics.recordIcaoCodesRequested(collection.size());
    }

    Timer.Sample sample = metrics.startLookupTimer();
    String outcome = "error";

    try {
      Object result = joinPoint.proceed();

      outcome =
          (result instanceof Collection<?> collection && collection.isEmpty())
              ? "empty"
              : "success";

      if (result instanceof Collection<?> collection) {
        metrics.recordAirportsReturned(collection.size());
      }

      return result;
    } catch (AeroLinkException ex) {
      if (ex.getErrorCode() == ErrorCode.UPSTREAM_RATE_LIMIT_EXCEEDED) {
        metrics.recordRateLimitHit();
      }
      throw ex;
    } finally {
      metrics.recordLookupRequest(outcome);
      metrics.stopLookupTimer(sample, outcome);
    }
  }
}
