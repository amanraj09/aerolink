package com.aerolink.model.response;

/**
 * Represents a single runway at an airport, including its physical and surface characteristics.
 *
 * @param id          Runway identifier
 * @param dimension   Physical dimensions of the runway
 * @param surface     Surface type code
 * @param alignment   Magnetic alignment of the runway
 */
public record RunwayDetail(
        String id,
        String dimension,
        String surface,
        Integer alignment
) {
}
