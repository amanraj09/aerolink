package com.aerolink.service;

import com.aerolink.model.response.AirportCommunications;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import com.aerolink.model.response.AirportLocation;
import com.aerolink.model.response.AirportOperations;
import com.aerolink.model.response.RunwayDetail;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer containing business logic for retrieving and transforming airport data.
 *
 * Acts as the intermediary between the controller and the external aviation data provider.
 * Responsible for orchestrating calls to the upstream API and mapping raw responses
 * into AeroLink's own response model.
 */
@Service
public class AeroLinkService {

    /**
     * Retrieves airport details for the given list of ICAO codes.
     *
     * @param icaoCodes list of 4-letter ICAO airport identifiers to look up
     * @return list of {@link AirportDetail} objects, one per requested ICAO code
     */
    public List<AirportDetail> getAirportDetails(List<String> icaoCodes) {
        return sampleAirportDetails();
    }

    /**
     * Returns hardcoded sample airport data for development and testing purposes.
     *
     * TODO: Replace with actual AviationDataProvider client integration.
     *
     * @return list of sample {@link AirportDetail} objects
     */
    private List<AirportDetail> sampleAirportDetails() {
        AirportDetail kmci = new AirportDetail(
                "KANSAS CITY/KANSAS CITY INTL",
                new AirportIdentifier("KMCI", "MCI", "MCI"),
                new AirportLocation("MO", "US", 39.2976, -94.7139, 313),
                new AirportOperations( "P", "T", "B", "S", "0", "9860"),
                new AirportCommunications("LCL/P,128.2;D-ATIS,128.375", "02E"),
                List.of(
                        new RunwayDetail("01L/19R", "10801x150", "C", 13),
                        new RunwayDetail("01R/19L", "9500x150", "C", 13),
                        new RunwayDetail("09/27", "9501x150", "A", 96)
                )
        );

        AirportDetail vhhh = new AirportDetail(
                "HONG KONG INTERNATIONAL AIRPORT",
                new AirportIdentifier("VHHH", "HKG", null),
                new AirportLocation("HK", "CN", 22.3116, 113.9225, 9),
                new AirportOperations("P", "T", "B", "S", null, "5650"),
                new AirportCommunications("ATIS,128.2;TWR,118.2", "3W"),
                List.of(
                        new RunwayDetail("07C/25C", "12468x197", "H", 74),
                        new RunwayDetail("07R/25L", "12468x197", "H", 74),
                        new RunwayDetail("07L/25R", "12468x197", "H", 74)
                )
        );

        return List.of(kmci, vhhh);
    }
}
