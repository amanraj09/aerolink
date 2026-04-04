package com.aerolink.controller;

import com.aerolink.model.response.AirportDetail;
import com.aerolink.service.AeroLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing AeroLink airport data endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AeroLinkController {

    private final AeroLinkService aeroLinkService;

    public AeroLinkController(AeroLinkService aeroLinkService) {
        this.aeroLinkService = aeroLinkService;
    }

    /**
     * Retrieves airport details for one or more ICAO codes.
     *
     * @param icaoCodes list of 4-letter ICAO airport identifiers
     * @return list of {@link AirportDetail} objects
     *
     */
    @GetMapping("/airport")
    public ResponseEntity<List<AirportDetail>> getAirportDetails(
            @RequestParam List<String> icaoCodes) {
        log.info("Client Requesting airport details for ICAO Codes : {}", icaoCodes);
        List<AirportDetail> airportDetails = aeroLinkService.getAirportDetails(icaoCodes);
        log.info("Airport details for ICAO Codes : {} successfully returned", icaoCodes);
        return ResponseEntity.ok(airportDetails);
    }
}
