package com.aerolink.controller;

import com.aerolink.model.response.AirportDetail;
import com.aerolink.service.AeroLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing AeroLink airport data endpoints.
 *
 * All endpoints are versioned under /api/v1 to support future
 * non-breaking API evolution.
 */
@RestController
@RequestMapping("/api/v1")
public class AeroLinkController {

    private final AeroLinkService aeroLinkService;

    /**
     * @param aeroLinkService service handling business logic for airport data retrieval
     */
    public AeroLinkController(AeroLinkService aeroLinkService) {
        this.aeroLinkService = aeroLinkService;
    }

    /**
     * Retrieves airport details for one or more ICAO codes.
     *
     * @param icaoCodes list of 4-letter ICAO airport identifiers (e.g. KMCI, EGLL)
     * @return 200 OK with a list of {@link AirportDetail} objects, one per ICAO code
     *
     * Example: GET /api/v1/airport?icaoCodes=KMCI,VHHH
     */
    @GetMapping("/airport")
    public ResponseEntity<List<AirportDetail>> getAirportDetails(
            @RequestParam List<String> icaoCodes) {
        return ResponseEntity.ok(aeroLinkService.getAirportDetails(icaoCodes));
    }
}
