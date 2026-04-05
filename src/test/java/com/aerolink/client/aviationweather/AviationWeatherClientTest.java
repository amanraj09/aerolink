package com.aerolink.client.aviationweather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aerolink.client.model.AviationWeatherRawResponse;
import com.aerolink.client.model.AviationWeatherRawResponse.RawRunway;
import com.aerolink.exception.AeroLinkException;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.error.ErrorCode;
import com.aerolink.model.response.AirportDetail;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class AviationWeatherClientTest {

  @Mock private RestClient restClient;
  @Mock private RestClient.RequestHeadersUriSpec uriSpec;
  @Mock private RestClient.RequestHeadersSpec headersSpec;
  @Mock private RestClient.ResponseSpec responseSpec;
  @Mock private Bucket bucket;
  @Mock private ConsumptionProbe consumptionProbe;
  @Mock private AeroLinkMetrics metrics;

  private CircuitBreaker circuitBreaker;
  private RetryTemplate noRetryTemplate;
  private AviationWeatherClient client;

  @BeforeEach
  void setUp() {
    circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    noRetryTemplate = buildRetryTemplate(1);
    client =
        new AviationWeatherClient(restClient, bucket, noRetryTemplate, circuitBreaker, metrics);

    lenient().when(restClient.get()).thenReturn(uriSpec);
    lenient().when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
    lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    lenient().when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
    lenient().when(consumptionProbe.isConsumed()).thenReturn(true);
  }

  // ─────────────────────────────────────────────
  // Rate Limiting
  // ─────────────────────────────────────────────

  @Nested
  class RateLimiting {

    @Test
    void rateLimitExceeded_throwsRateLimitExceeded_withRetryAfterSeconds() {
      when(consumptionProbe.isConsumed()).thenReturn(false);
      when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(5_000_000_000L);

      assertThatThrownBy(() -> client.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .isInstanceOf(AeroLinkException.class)
          .satisfies(
              ex -> {
                AeroLinkException aex = (AeroLinkException) ex;
                assertThat(aex.getErrorCode()).isEqualTo(ErrorCode.UPSTREAM_RATE_LIMIT_EXCEEDED);
                assertThat(aex.getRetryAfterSeconds()).isEqualTo(5L);
              });
    }

    @Test
    void rateLimitNotExceeded_proceedsToApiCall() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of()));

      List<AirportDetail> result = client.fetchAirportsByIcaoCodes(List.of("KJFK"));

      assertThat(result).isEmpty();
    }
  }

  // ─────────────────────────────────────────────
  // Successful Responses
  // ─────────────────────────────────────────────

  @Nested
  class SuccessfulResponse {

    @Test
    void returns200WithData_returnsMappedAirportDetails() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of(buildRawResponse("KJFK", "John F Kennedy Intl"))));

      List<AirportDetail> result = client.fetchAirportsByIcaoCodes(List.of("KJFK"));

      assertThat(result).hasSize(1);
      assertThat(result.get(0).airportName()).isEqualTo("John F Kennedy Intl");
      assertThat(result.get(0).identifier().icaoId()).isEqualTo("KJFK");
    }

    @Test
    void returns200WithMultipleAirports_returnsMappedList() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(
              ResponseEntity.ok(
                  List.of(
                      buildRawResponse("KJFK", "John F Kennedy Intl"),
                      buildRawResponse("KLAX", "Los Angeles Intl"))));

      List<AirportDetail> result = client.fetchAirportsByIcaoCodes(List.of("KJFK", "KLAX"));

      assertThat(result).hasSize(2);
      assertThat(result.get(0).identifier().icaoId()).isEqualTo("KJFK");
      assertThat(result.get(1).identifier().icaoId()).isEqualTo("KLAX");
    }

    @Test
    void returns200WithEmptyBody_returnsEmptyList() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of()));

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK"))).isEmpty();
    }

    @Test
    void returns200WithNullBody_returnsEmptyList() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(null));

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK"))).isEmpty();
    }

    @Test
    void returns204_returnsEmptyList() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.noContent().build());

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK"))).isEmpty();
    }
  }

  // ─────────────────────────────────────────────
  // Response Mapping
  // ─────────────────────────────────────────────

  @Nested
  class ResponseMapping {

    @Test
    void mapsAllFieldsCorrectly() {
      AviationWeatherRawResponse raw =
          new AviationWeatherRawResponse(
              "KJFK",
              "JFK",
              "JFK",
              "John F Kennedy Intl",
              "NY",
              "US",
              40.6398,
              -73.7789,
              13,
              "14W",
              "PU",
              "Y",
              "Y",
              "Full",
              "Civil",
              "Y",
              "119.1",
              null);
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of(raw)));

      AirportDetail result = client.fetchAirportsByIcaoCodes(List.of("KJFK")).get(0);

      assertThat(result.airportName()).isEqualTo("John F Kennedy Intl");
      assertThat(result.identifier().icaoId()).isEqualTo("KJFK");
      assertThat(result.identifier().iataId()).isEqualTo("JFK");
      assertThat(result.identifier().faaId()).isEqualTo("JFK");
      assertThat(result.location().state()).isEqualTo("NY");
      assertThat(result.location().country()).isEqualTo("US");
      assertThat(result.location().latitude()).isEqualTo(40.6398);
      assertThat(result.location().longitude()).isEqualTo(-73.7789);
      assertThat(result.location().elevationFt()).isEqualTo(13);
      assertThat(result.communications().magneticDeclination()).isEqualTo("14W");
      assertThat(result.runways()).isEmpty();
    }

    @Test
    void trimsWhitespaceFromName() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(
              ResponseEntity.ok(List.of(buildRawResponse("KJFK", "  John F Kennedy Intl  "))));

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK")).get(0).airportName())
          .isEqualTo("John F Kennedy Intl");
    }

    @Test
    void nullName_mappedAsNull() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of(buildRawResponse("KJFK", null))));

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK")).get(0).airportName()).isNull();
    }

    @Test
    void nullRunways_mappedAsEmptyList() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of(buildRawResponse("KJFK", "Kennedy"))));

      assertThat(client.fetchAirportsByIcaoCodes(List.of("KJFK")).get(0).runways()).isEmpty();
    }

    @Test
    void mapsRunwaysCorrectly() {
      RawRunway runway = new RawRunway("01L/19R", "10801x150", "C", 13);
      AviationWeatherRawResponse raw =
          new AviationWeatherRawResponse(
              "KJFK",
              null,
              null,
              "Kennedy",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              List.of(runway));
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(List.of(raw)));

      AirportDetail result = client.fetchAirportsByIcaoCodes(List.of("KJFK")).get(0);

      assertThat(result.runways()).hasSize(1);
      assertThat(result.runways().get(0).id()).isEqualTo("01L/19R");
      assertThat(result.runways().get(0).dimension()).isEqualTo("10801x150");
      assertThat(result.runways().get(0).surface()).isEqualTo("C");
      assertThat(result.runways().get(0).alignment()).isEqualTo(13);
    }
  }

  // ─────────────────────────────────────────────
  // Error Handling
  // ─────────────────────────────────────────────

  @Nested
  class ErrorHandling {

    @Test
    void upstream4xxResponse_throwsUpstreamClientError() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenThrow(new AeroLinkException(ErrorCode.UPSTREAM_CLIENT_ERROR));

      assertThatThrownBy(() -> client.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .isInstanceOf(AeroLinkException.class)
          .satisfies(
              ex ->
                  assertThat(((AeroLinkException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.UPSTREAM_CLIENT_ERROR));
    }

    @Test
    void upstream500Response_throwsUpstreamServerError() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenThrow(new AeroLinkException(ErrorCode.UPSTREAM_SERVER_ERROR));

      assertThatThrownBy(() -> client.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .isInstanceOf(AeroLinkException.class)
          .satisfies(
              ex ->
                  assertThat(((AeroLinkException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.UPSTREAM_SERVER_ERROR));
    }

    @Test
    void networkError_throwsTemporarilyUnavailable() {
      when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
          .thenThrow(new ResourceAccessException("Connection refused"));

      assertThatThrownBy(() -> client.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .isInstanceOf(AeroLinkException.class)
          .satisfies(
              ex ->
                  assertThat(((AeroLinkException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR));
    }

    @Test
    void circuitBreakerOpen_throwsTemporarilyUnavailable() {
      circuitBreaker.transitionToOpenState();

      assertThatThrownBy(() -> client.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .isInstanceOf(AeroLinkException.class)
          .satisfies(
              ex ->
                  assertThat(((AeroLinkException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.UPSTREAM_API_TEMPORARILY_UNAVAILABLE_ERROR));
    }
  }

  // ─────────────────────────────────────────────
  // Provider Name
  // ─────────────────────────────────────────────

  @Nested
  class ProviderName {

    @Test
    void getProviderName_returnsAviationWeather() {
      assertThat(client.getProviderName()).isEqualTo("aviationWeather");
    }
  }

  // ─────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────

  private AviationWeatherRawResponse buildRawResponse(String icaoId, String name) {
    return new AviationWeatherRawResponse(
        icaoId, null, null, name, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null);
  }

  private RetryTemplate buildRetryTemplate(int maxAttempts) {
    RetryTemplate template = new RetryTemplate();
    template.setRetryPolicy(
        new SimpleRetryPolicy(maxAttempts, Map.of(RestClientException.class, true)));
    return template;
  }
}
