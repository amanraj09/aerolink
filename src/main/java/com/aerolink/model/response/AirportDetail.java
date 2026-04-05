package com.aerolink.model.response;

import java.util.List;

/**
 * Top-level response object representing a single airport returned by the AeroLink API.
 *
 * <p>Aggregates all airport-related information into logical sub-objects to keep the response clean
 * and navigable for API consumers.
 *
 * @param airportName Full official name of the airport (e.g. "KANSAS CITY/KANSAS CITY INTL")
 * @param identifier Codes used to identify the airport across different aviation systems
 * @param location Geographic details including coordinates and elevation
 * @param operations Operational attributes such as owner and tower available
 * @param communications Radio frequencies and magnetic declination data
 * @param runways List of runways at the airport with physical and surface details
 */
public record AirportDetail(
    String airportName,
    AirportIdentifier identifier,
    AirportLocation location,
    AirportOperations operations,
    AirportCommunications communications,
    List<RunwayDetail> runways) {}
