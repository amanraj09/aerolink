package com.aerolink.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"aviation.provider.api.base-url=http://localhost:8089"})
@WireMockTest(httpPort = 8089)
class UpstreamRateLimitIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    restTemplate.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory());
  }

  @Test
  void upstreamReturns429_ourApiConvertsItToTooManyRequestsError() {
    // Arrange: Stub the upstream fake aviation server to return a 429 Too Many
    // Requests
    stubFor(
        get(urlPathEqualTo("/airport"))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "1")));

    String url =
        UriComponentsBuilder.fromPath("/api/v1/airport")
            .queryParam("icaoCodes", "KJFK")
            .toUriString();

    // Act
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getBody()).contains("AERO-104");
    assertThat(response.getHeaders().containsKey("Retry-After")).isTrue();
    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
  }
}
