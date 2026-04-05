package com.aerolink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"aviation.provider.api.request-limit-per-minute=1"})
class RateLimitIntegrationTest {

  private static final String BASE_URL = "/api/v1/airport";

  @Autowired private TestRestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    restTemplate.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory());
  }

  @Test
  void rateLimitExceeded_returns429TooManyRequests() {
    String url =
        UriComponentsBuilder.fromPath(BASE_URL).queryParam("icaoCodes", "KJFK").toUriString();

    // Request 1 - should pass (consumes the 1 allowed token)
    ResponseEntity<String> response1 = restTemplate.getForEntity(url, String.class);
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Request 2 - should fail (bucket is empty and needs 1 entire minute to fully
    // refill)
    ResponseEntity<String> response2 = restTemplate.getForEntity(url, String.class);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response2.getBody()).contains("AERO-104");
    assertThat(response2.getHeaders().containsKey("Retry-After")).isTrue();
  }
}
