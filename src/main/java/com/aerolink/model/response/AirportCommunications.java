package com.aerolink.model.response;

/**
 * Contains radio communication and navigation reference data for the airport.
 *
 * @param frequencies           List of radio frequencies used at the airport,
 * @param magneticDeclination   Magnetic declination at the airport
 */
public record AirportCommunications(
        String frequencies,
        String magneticDeclination
) {
}
