package com.aerolink.metrics;

import com.aerolink.model.error.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Central registry of all custom business metrics for AeroLink.
 *
 * <p>All meters are pre-registered eagerly in the constructor so they appear in the Prometheus
 * scrape from app startup with value {@code 0}, rather than only after first use.
 *
 * <p>Metric catalogue:
 *
 * <ul>
 *   <li>{@code aerolink.airport.lookups} — Counter: total lookup requests by provider and outcome
 *   <li>{@code aerolink.airport.lookup.duration} — Timer: end-to-end lookup latency by provider and
 *       outcome
 *   <li>{@code aerolink.airport.lookup.icao.codes} — DistributionSummary: ICAO codes per request
 *   <li>{@code aerolink.airport.lookup.airports.returned} — DistributionSummary: airports returned
 *       per request
 *   <li>{@code aerolink.errors} — Counter: application errors by error code and type
 *   <li>{@code aerolink.upstream.rate.limit.hits} — Counter: upstream rate limit rejections by
 *       provider
 * </ul>
 */
@Component
public class AeroLinkMetrics {

  private static final String LOOKUP_REQUESTS = "aerolink.airport.lookups";
  private static final String LOOKUP_DURATION = "aerolink.airport.lookup.duration";
  private static final String ICAO_CODES_PER_REQUEST = "aerolink.airport.lookup.icao.codes";
  private static final String AIRPORTS_RETURNED = "aerolink.airport.lookup.airports.returned";
  private static final String ERRORS = "aerolink.errors";
  private static final String RATE_LIMIT_HITS = "aerolink.upstream.rate.limit.hits";

  private static final List<String> OUTCOMES = List.of("success", "empty", "error");

  // Pre-registered meters — keyed by "provider:outcome" for lookup/timer,
  // by ErrorCode.getCode() for errors
  private final Map<String, Counter> lookupCounters;
  private final Map<String, Timer> lookupTimers;
  private final Map<String, Counter> errorCounters;
  private final Counter rateLimitHitCounter;
  private final DistributionSummary icaoCodesPerRequest;
  private final DistributionSummary airportsReturned;

  public AeroLinkMetrics(
      MeterRegistry meterRegistry, @Value("${aerolink.provider}") String activeProvider) {

    // ── Lookup counters (provider × outcome) ────────────────────────────────
    lookupCounters =
        OUTCOMES.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    outcome ->
                        Counter.builder(LOOKUP_REQUESTS)
                            .description(
                                "Total number of airport lookup requests by provider and outcome")
                            .tag("provider", activeProvider)
                            .tag("outcome", outcome)
                            .register(meterRegistry)));

    // ── Lookup timers (provider × outcome) ──────────────────────────────────
    lookupTimers =
        OUTCOMES.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    outcome ->
                        Timer.builder(LOOKUP_DURATION)
                            .description("End-to-end duration of airport lookup requests")
                            .tag("provider", activeProvider)
                            .tag("outcome", outcome)
                            .register(meterRegistry)));

    // ── Error counters (one per ErrorCode) ──────────────────────────────────
    errorCounters =
        Stream.of(ErrorCode.values())
            .collect(
                Collectors.toMap(
                    ErrorCode::getCode,
                    errorCode ->
                        Counter.builder(ERRORS)
                            .description(
                                "Total number of application errors by error code and type")
                            .tag("error_code", errorCode.getCode())
                            .tag("error_type", resolveErrorType(errorCode))
                            .register(meterRegistry)));

    // ── Rate limit hit counter ───────────────────────────────────────────────
    rateLimitHitCounter =
        Counter.builder(RATE_LIMIT_HITS)
            .description("Number of requests blocked due to upstream API rate limiting")
            .tag("provider", activeProvider)
            .register(meterRegistry);

    // ── Distribution summaries ───────────────────────────────────────────────
    icaoCodesPerRequest =
        DistributionSummary.builder(ICAO_CODES_PER_REQUEST)
            .description("Distribution of ICAO codes per airport lookup request")
            .baseUnit("codes")
            .register(meterRegistry);

    airportsReturned =
        DistributionSummary.builder(AIRPORTS_RETURNED)
            .description("Distribution of airports returned per lookup request")
            .baseUnit("airports")
            .register(meterRegistry);
  }

  /**
   * Increments the airport lookup request counter.
   *
   * @param outcome {@code "success"}, {@code "empty"}, or {@code "error"}
   */
  public void recordLookupRequest(String outcome) {
    lookupCounters.get(outcome).increment();
  }

  /**
   * Records the number of ICAO codes submitted in a single lookup request.
   *
   * @param count number of ICAO codes
   */
  public void recordIcaoCodesRequested(int count) {
    icaoCodesPerRequest.record(count);
  }

  /**
   * Records the number of airports returned from a lookup.
   *
   * @param count number of airports in the response
   */
  public void recordAirportsReturned(int count) {
    airportsReturned.record(count);
  }

  /**
   * Starts a timer sample to measure lookup duration. Must be paired with {@link
   * #stopLookupTimer(Timer.Sample, String)}.
   *
   * @return a started {@link Timer.Sample}
   */
  public Timer.Sample startLookupTimer() {
    return Timer.start();
  }

  /**
   * Stops the timer sample and records it under the given outcome tag.
   *
   * @param sample the sample returned by {@link #startLookupTimer()}
   * @param outcome {@code "success"}, {@code "empty"}, or {@code "error"}
   */
  public void stopLookupTimer(Timer.Sample sample, String outcome) {
    sample.stop(lookupTimers.get(outcome));
  }

  /**
   * Increments the error counter for a given {@link ErrorCode}.
   *
   * @param errorCode the error code that was raised
   */
  public void recordError(ErrorCode errorCode) {
    errorCounters.get(errorCode.getCode()).increment();
  }

  /** Increments the upstream rate-limit hit counter. */
  public void recordRateLimitHit() {
    rateLimitHitCounter.increment();
  }

  private String resolveErrorType(ErrorCode errorCode) {
    int status = errorCode.getHttpStatus().value();
    if (status == 429) return "rate_limit";
    if (status >= 400 && status < 500) return "validation";
    if (errorCode.name().startsWith("UPSTREAM")) return "upstream";
    return "internal";
  }
}
