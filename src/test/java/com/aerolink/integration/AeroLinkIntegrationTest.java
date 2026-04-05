package com.aerolink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Integration tests for the AeroLink API.
 *
 * <p>Starts a real embedded server and makes actual HTTP calls. Upstream Aviation Weather API calls
 * are also real — no mocking.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AeroLinkIntegrationTest {

  private static final String BASE_URL = "/api/v1/airport";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void validIcaoCode_returns200WithAirportDetails() {
    String url =
        UriComponentsBuilder.fromPath(BASE_URL)
            .queryParam("icaoCodes", "KJFK")
            .build()
            .toUriString();

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("KJFK");
  }

  @Test
  void missingIcaoCodes_returns400WithAero101() {
    ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("AERO-101");
  }

  @Test
  void icaoCodesExceedLimit_returns400WithAero102() {
    String url =
        UriComponentsBuilder.fromPath(BASE_URL)
            .queryParam(
                "icaoCodes",
                "KAAA",
                "KAAB",
                "KAAC",
                "KAAD",
                "KAAE",
                "KAAF",
                "KAAG",
                "KAAH",
                "KAAI",
                "KAAJ",
                "KAAK",
                "KAAL",
                "KAAM",
                "KAAN",
                "KAAO",
                "KAAP")
            .build()
            .toUriString();

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("AERO-102");
  }

  @Test
  void invalidIcaoFormat_returns400WithAero103() {
    String url =
        UriComponentsBuilder.fromPath(BASE_URL)
            .queryParam("icaoCodes", "KJF1")
            .build()
            .toUriString();

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("AERO-103");
  }
}
