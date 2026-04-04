package com.aerolink.model.response;

/**
 * Holds the industry-standard identifiers used to reference an airport
 * across different aviation systems and databases.
 *
 * @param icaoId  4-letter ICAO code
 * @param iataId  3-letter IATA code
 * @param faaId   FAA identifier used within the US national airspace system
 */
public record AirportIdentifier(
        String icaoId,
        String iataId,
        String faaId
) {
}
