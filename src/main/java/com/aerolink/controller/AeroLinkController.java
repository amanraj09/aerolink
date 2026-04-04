package com.aerolink.controller;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.model.error.ErrorCode;
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

    private static final int MAX_ICAO_CODES = 15;

    private final AeroLinkService aeroLinkService;

    public AeroLinkController(AeroLinkService aeroLinkService) {
        this.aeroLinkService = aeroLinkService;
    }

    /**
     * Retrieves airport details for one or more ICAO codes.
     * Maximum of 15 ICAO codes allowed per request.
     *
     * @param icaoCodes list of 4-letter ICAO airport identifiers (max 15)
     * @return 200 with list of {@link AirportDetail}
     */
    @GetMapping("/airport")
    public ResponseEntity<List<AirportDetail>> getAirportDetails(@RequestParam List<String> icaoCodes) {
        log.info("Received request for {} ICAO code(s): {}", icaoCodes.size(), icaoCodes);

        if (icaoCodes.size() > MAX_ICAO_CODES) {
            log.error("Request rejected — {} ICAO codes exceeds limit of {}", icaoCodes.size(), MAX_ICAO_CODES);
            throw new AeroLinkException(ErrorCode.ICAO_LIMIT_EXCEEDED,
                    "Maximum allowed is " + MAX_ICAO_CODES + ", but received " + icaoCodes.size());
        }

        List<AirportDetail> airportDetails = aeroLinkService.getAirportDetails(icaoCodes);
        log.info("Successfully returned airport details for {} ICAO code(s)", icaoCodes.size());
        return ResponseEntity.ok(airportDetails);
    }
}
