package com.aerolink.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the raw JSON response from aviationweather.gov /api/data/airport endpoint.
 *
 * <p>The API returns a JSON array — the client deserialises as List<AviationWeatherRawResponse>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AviationWeatherRawResponse(
    String icaoId,
    String iataId,
    String faaId,
    String name,
    String state,
    String country,
    Double lat,
    Double lon,
    Integer elev,
    String magdec,
    String owner,
    String tower,
    String beacon,
    String services,
    String operations,
    String passengers,
    String freqs,
    List<RawRunway> runways) {
  /**
   * Represents a single runway entry as returned by the upstream API.
   *
   * @param id Runway identifier (e.g. "01L/19R")
   * @param dimension Length x width in feet (e.g. "10801x150")
   * @param surface Surface type code ("C" = concrete, "A" = asphalt, "H" = hard)
   * @param alignment Magnetic alignment in degrees
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RawRunway(String id, String dimension, String surface, Integer alignment) {}
}
