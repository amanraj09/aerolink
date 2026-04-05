package com.aerolink.model.response;

import lombok.Getter;

/** Represents the ownership type of an airport. */
@Getter
public enum AirportOwnership {
  PUBLIC("P", "Publicly owned"),
  PRIVATE("R", "Private"),
  MILITARY("M", "Military"),
  UNKNOWN("U", "Unknown");

  private final String code;
  private final String description;

  AirportOwnership(String code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Maps an upstream string code to an {@link AirportOwnership} enum.
   *
   * @param code raw code from upstream (e.g. "P", "R", "M")
   * @return matching enum or UNKNOWN if no match found
   */
  public static AirportOwnership fromCode(String code) {
    if (code == null) {
      return UNKNOWN;
    }
    for (AirportOwnership ownership : values()) {
      if (ownership.code.equalsIgnoreCase(code.trim())) {
        return ownership;
      }
    }
    return UNKNOWN;
  }
}
