package com.aerolink.client.aviationweather;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.client.model.AviationWeatherRawResponse;
import com.aerolink.constant.AeroLinkConstants;
import com.aerolink.exception.AeroLinkException;
import com.aerolink.model.error.ErrorCode;
import com.aerolink.model.response.AirportCommunications;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import com.aerolink.model.response.AirportLocation;
import com.aerolink.model.response.AirportOperations;
import com.aerolink.model.response.RunwayDetail;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * RestClient-based implementation of {@link AviationDataProvider} for public Aviation Weather API.
 */
@Slf4j
@Component
public class AviationWeatherClient implements AviationDataProvider {

  private final String AIRPORT_PATH = "/airport";
  private final int NANOS_PER_SECOND = 1000000000;
  private final String PROVIDER_NAME = "aviationWeather";

  private final RestClient restClient;
  private final Bucket rateLimiterBucket;
  private final RetryTemplate retryTemplate;
  private final CircuitBreaker circuitBreaker;

  @Autowired
  public AviationWeatherClient(
      @Qualifier(AeroLinkConstants.PROVIDER_REST_CLIENT) RestClient aviationWeatherRestClient,
      @Qualifier(AeroLinkConstants.PROVIDER_RATE_LIMITER) Bucket rateLimiterBucket,
      @Qualifier(AeroLinkConstants.PROVIDER_RETRY) RetryTemplate retryTemplate,
      @Qualifier(AeroLinkConstants.PROVIDER_CIRCUIT_BREAKER)
          CircuitBreaker aviationWeatherCircuitBreaker) {
    this.restClient = aviationWeatherRestClient;
    this.rateLimiterBucket = rateLimiterBucket;
    this.retryTemplate = retryTemplate;
    this.circuitBreaker = aviationWeatherCircuitBreaker;
  }

  /**
   * Fetches airport details for the given ICAO codes in a single upstream API call.
   *
   * @param icaoCodes list of ICAO codes to look up
   * @return list of mapped {@link AirportDetail} objects
   */
  @Override
  public List<AirportDetail> fetchAirportsByIcaoCodes(List<String> icaoCodes) {
    String ids = String.join(",", icaoCodes);

    ConsumptionProbe probe = rateLimiterBucket.tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      long retryAfterSeconds = probe.getNanosToWaitForRefill() / NANOS_PER_SECOND;
      log.warn("Aviation Weather API rate limit reached. Retry after {}s", retryAfterSeconds);
      throw new AeroLinkException(ErrorCode.UPSTREAM_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
    }

    try {
      return circuitBreaker.executeSupplier(
          () ->
              retryTemplate.execute(
                  context -> {
                    log.info(
                        "Attempt {} — calling Aviation Weather API for IDs: {}",
                        context.getRetryCount() + 1,
                        ids);

                    ResponseEntity<List<AviationWeatherRawResponse>> response =
                        restClient
                            .get()
                            .uri(
                                uriBuilder ->
                                    uriBuilder
                                        .path(AIRPORT_PATH)
                                        .queryParam("format", "json")
                                        .queryParam("ids", ids)
                                        .build())
                            .retrieve()
                            .onStatus(
                                HttpStatusCode::is4xxClientError,
                                (request, apiResponse) -> {
                                  log.error(
                                      "Aviation Weather API returned HTTP {} for IDs: {} — not retrying",
                                      apiResponse.getStatusCode().value(),
                                      ids);
                                  throw new AeroLinkException(ErrorCode.UPSTREAM_CLIENT_ERROR);
                                })
                            .onStatus(
                                status -> status.value() == 500,
                                (request, apiResponse) -> {
                                  log.error(
                                      "Aviation Weather API returned HTTP 500 for IDs: {} — not retrying",
                                      ids);
                                  throw new AeroLinkException(ErrorCode.UPSTREAM_SERVER_ERROR);
                                })
                            .toEntity(new ParameterizedTypeReference<>() {});

                    HttpStatusCode statusCode = response.getStatusCode();
                    List<AviationWeatherRawResponse> rawResponses = response.getBody();

                    if (statusCode.value() == 204 || CollectionUtils.isEmpty(rawResponses)) {
                      log.warn(
                          "Aviation Weather API returned HTTP {} with no results for IDs: {}",
                          statusCode.value(),
                          ids);
                      return List.of();
                    }

                    log.info(
                        "Aviation Weather API returned HTTP {} with {} result(s) for IDs: {}",
                        statusCode.value(),
                        rawResponses.size(),
                        ids);
                    return rawResponses.stream().map(this::mapToAirportDetail).toList();
                  },
                  context -> {
                    log.error(
                        "All {} attempt(s) exhausted for IDs: {}", context.getRetryCount(), ids);
                    throw new AeroLinkException(
                        ErrorCode.UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR);
                  }));
    } catch (RestClientException | CallNotPermittedException ex) {
      throw handleUpstreamException(ex, ids);
    }
  }

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  private AeroLinkException handleUpstreamException(Exception ex, String ids) {
    if (ex instanceof CallNotPermittedException) {
      log.error("Circuit breaker is OPEN — upstream calls blocked for IDs: {}", ids);
      return new AeroLinkException(ErrorCode.UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR);
    }
    if (ex instanceof HttpServerErrorException e) {
      log.error(
          "Upstream returned HTTP {} for IDs: {} — retries exhausted",
          e.getStatusCode().value(),
          ids);
      return new AeroLinkException(ErrorCode.UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR);
    }
    if (ex instanceof ResourceAccessException) {
      log.error("Network failure reaching upstream for IDs: {}", ids);
      return new AeroLinkException(ErrorCode.UPSTREAM_SERVER_ERROR);
    }
    log.error("Upstream communication error for IDs: {} — {}", ids, ex.getMessage());
    return new AeroLinkException(ErrorCode.UPSTREAM_RESPONSE_PARSE_ERROR);
  }

  /**
   * Maps a raw upstream API response to provider-agnostic {@link AirportDetail}. Mapping here in
   * the client layer so the service layer remains client schema agnostic. If the API schema
   * changes, only this method needs updating.
   *
   * @param response response from the upstream API
   * @return mapped AirportDetail
   */
  private AirportDetail mapToAirportDetail(AviationWeatherRawResponse response) {
    List<RunwayDetail> runways =
        response.runways() == null
            ? List.of()
            : response.runways().stream()
                .map(r -> new RunwayDetail(r.id(), r.dimension(), r.surface(), r.alignment()))
                .toList();

    return new AirportDetail(
        response.name() != null ? response.name().trim() : null,
        new AirportIdentifier(response.icaoId(), response.iataId(), response.faaId()),
        new AirportLocation(
            response.state(), response.country(), response.lat(), response.lon(), response.elev()),
        new AirportOperations(
            response.owner(),
            response.tower(),
            response.beacon(),
            response.services(),
            response.operations(),
            response.passengers()),
        new AirportCommunications(response.freqs(), response.magdec()),
        runways);
  }
}
