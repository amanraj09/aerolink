package com.aerolink.service;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.model.response.AirportDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer containing business logic for retrieving and transforming airport data.
 *
 * Acts as the intermediary between the controller and the external aviation data provider.
 * Delegates the upstream API call to {@link AviationDataProvider}, keeping the service
 * decoupled from any specific provider implementation.
 */
@Slf4j
@Service
public class AeroLinkService {

    private final AviationDataProvider aviationDataProvider;

    public AeroLinkService(AviationDataProvider aviationDataProvider) {
        this.aviationDataProvider = aviationDataProvider;
    }

    /**
     * Retrieves airport details for the given list of ICAO codes.
     *
     * @param icaoCodes list of 4-letter ICAO airport identifiers
     * @return list of {@link AirportDetail} objects
     */
    public List<AirportDetail> getAirportDetails(List<String> icaoCodes) {
        log.info("Fetching airport details for {} ICAO code(s): {}", icaoCodes.size(), icaoCodes);
        return aviationDataProvider.fetchAirportsByIcaoCodes(icaoCodes);
    }
}
