package com.aerolink.model.response;

/**
 * Represents the geographic location of an airport.
 *
 * @param state State or region code
 * @param country ISO 2-letter country code
 * @param latitude Decimal degrees latitude of the airport
 * @param longitude Decimal degrees longitude of the airport
 * @param elevationFt Elevation of the airport above mean sea level, in feet
 */
public record AirportLocation(
    String state, String country, Double latitude, Double longitude, Integer elevationFt) {}
