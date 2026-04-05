package com.aerolink.controller;

import com.aerolink.model.response.AirportDetail;
import com.aerolink.service.AeroLinkService;
import com.aerolink.util.RequestValidator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing AeroLink airport data endpoints. */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AeroLinkController {

  private final AeroLinkService aeroLinkService;
  private final RequestValidator requestValidator;

  public AeroLinkController(AeroLinkService aeroLinkService, RequestValidator requestValidator) {
    this.aeroLinkService = aeroLinkService;
    this.requestValidator = requestValidator;
  }

  /**
   * Retrieves airport details for one or more ICAO codes. Maximum of 15 ICAO codes allowed per
   * request.
   *
   * @param icaoCodes list of 4-letter ICAO airport identifiers (max 15)
   * @return 200 with list of {@link AirportDetail}
   */
  @GetMapping("/airport")
  public ResponseEntity<List<AirportDetail>> getAirportDetails(
      @RequestParam List<String> icaoCodes) {
    requestValidator.validateAirportDetailsRequest(icaoCodes);
    log.info(
        "Received request for {} ICAO code(s): {}",
        icaoCodes == null ? 0 : icaoCodes.size(),
        icaoCodes);

    List<AirportDetail> airportDetails = aeroLinkService.getAirportDetails(icaoCodes);
    log.info("Successfully returned airport details for {} ICAO code(s)", icaoCodes.size());
    return ResponseEntity.ok(airportDetails);
  }
}
