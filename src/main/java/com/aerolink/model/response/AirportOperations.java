package com.aerolink.model.response;

/**
 * Describes the operational characteristics and facilities available at an airport.
 *
 * @param owner Ownership type of the airport (Public, Private, Military)
 * @param isTowerAvailable Indicates if a control tower is present
 * @param isBeaconAvailable Indicates if an airport beacon is present
 * @param passengerCount Annual passenger count
 */
public record AirportOperations(
    AirportOwnership owner,
    boolean isTowerAvailable,
    boolean isBeaconAvailable,
    String passengerCount) {}
