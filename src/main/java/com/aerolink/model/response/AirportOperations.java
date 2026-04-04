package com.aerolink.model.response;

/**
 * Describes the operational characteristics and facilities available at an airport.
 *
 * @param owner       Ownership type — "P" for public, "R" for private
 * @param tower       Indicates presence of a control tower
 * @param beacon      Indicates presence of an airport beacon
 * @param services    Available services indicator
 * @param operations  Operational status code of the airport
 * @param passengers  Annual passenger count
 */
public record AirportOperations(
        String owner,
        String tower,
        String beacon,
        String services,
        String operations,
        String passengers
) {
}
