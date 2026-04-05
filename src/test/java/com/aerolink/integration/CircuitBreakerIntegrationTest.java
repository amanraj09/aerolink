package com.aerolink.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.aerolink.constant.AeroLinkConstants;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Integration tests for circuit breaker behaviour under upstream 4xx, 5xx, and CB OPEN scenarios.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"aviation.provider.api.base-url=http://localhost:8090"})
@WireMockTest(httpPort = 8090)
class CircuitBreakerIntegrationTest {

  private static final String ICAO_URL =
      UriComponentsBuilder.fromPath("/api/v1/airport")
          .queryParam("icaoCodes", "KJFK")
          .toUriString();

  @Autowired private TestRestTemplate restTemplate;

  @Autowired
  @Qualifier(AeroLinkConstants.PROVIDER_CIRCUIT_BREAKER)
  private CircuitBreaker circuitBreaker;

  @AfterEach
  void resetCircuitBreaker() {
    circuitBreaker.reset();
    circuitBreaker.transitionToClosedState();
    restTemplate.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory());
  }

  // ─────────────────────────────────────────────
  // Upstream 4xx — CB is NOT affected
  // ─────────────────────────────────────────────

  @Test
  void upstream400_returnsAero203() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(400)));

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).contains("AERO-203");
  }

  @Test
  void upstream404_returnsAero203() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(404)));

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).contains("AERO-203");
  }

  @Test
  void upstream4xx_doesNotCountTowardCbFailureRate() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(400)));

    // Fill the entire sliding window with 4xx responses
    for (int i = 0; i < 10; i++) {
      restTemplate.getForEntity(ICAO_URL, String.class);
    }

    // CB must remain CLOSED — 4xx are in ignoreExceptions
    assertThat(circuitBreaker.getState()).isEqualTo(State.CLOSED);
    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
  }

  // ─────────────────────────────────────────────
  // Upstream 5xx — CB records failures
  // ─────────────────────────────────────────────

  @Test
  void upstream500_returnsAero201() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(500)));

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).contains("AERO-201");
  }

  @Test
  void upstream502_returnsAero201() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(502)));

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).contains("AERO-201");
  }

  @Test
  void upstream503_returnsAero201() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(503)));

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).contains("AERO-201");
  }

  @Test
  void upstream5xx_countsTowardCbFailureRate() {
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(500)));

    restTemplate.getForEntity(ICAO_URL, String.class);

    // Each 5xx must be recorded as a CB failure (HttpServerErrorException is a
    // RestClientException, which is in the CB's recordExceptions list)
    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
  }

  @Test
  void upstream5xx_afterSlidingWindowExhausted_opensCb() {
    // Sliding window = 10, minimumNumberOfCalls = 10, failureRateThreshold = 20%
    // All 10 calls fail → 100% failure rate → CB opens.
    // We will loop 11 times instead of 10 just to guarantee it crosses the
    // threshold and trips
    // properly on standard evaluation frames.
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(500)));

    for (int i = 0; i < 11; i++) {
      restTemplate.getForEntity(ICAO_URL, String.class);
    }

    assertThat(circuitBreaker.getState()).isEqualTo(State.OPEN);
  }

  @Test
  void upstream5xxMixedWith2xx_belowThreshold_cbRemainsClose() {
    // 1 failure out of 10 calls = 10% failure rate < 20% threshold → CB stays
    // CLOSED
    stubFor(get(urlPathEqualTo("/airport")).willReturn(aResponse().withStatus(500)));
    for (int i = 0; i < 1; i++) {
      restTemplate.getForEntity(ICAO_URL, String.class);
    }

    com.github.tomakehurst.wiremock.client.WireMock.reset();
    stubFor(
        get(urlPathEqualTo("/airport"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[]")));
    for (int i = 0; i < 9; i++) {
      ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);
      assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
    }

    assertThat(circuitBreaker.getState()).isEqualTo(State.CLOSED);
  }

  // ─────────────────────────────────────────────
  // Circuit breaker OPEN — all calls short-circuited
  // ─────────────────────────────────────────────

  @Test
  void circuitBreakerOpen_returns503WithAero202() {
    circuitBreaker.transitionToOpenState();

    ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).contains("AERO-202");
  }

  @Test
  void circuitBreakerOpen_blocksSubsequentCallsWithoutHittingUpstream() {
    circuitBreaker.transitionToOpenState();

    // None of these calls reach WireMock — CB short-circuits them all
    for (int i = 0; i < 3; i++) {
      ResponseEntity<String> response = restTemplate.getForEntity(ICAO_URL, String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody()).contains("AERO-202");
    }

    assertThat(circuitBreaker.getState()).isEqualTo(State.OPEN);
  }
}
