package com.aerolink.client;

import com.aerolink.model.response.AirportDetail;
import java.util.List;

/**
 * Abstraction over any aviation data provider to decouple application from specific aviation data
 * provider vendor.
 */
public interface AviationDataProvider {

  /**
   * Fetches airport details for one or more ICAO codes in a single request.
   *
   * @param icaoCodes list of 4-letter ICAO airport identifiers (e.g. ["KMCI", "VHHH"])
   * @return list of airport details, one per matched ICAO code
   */
  List<AirportDetail> fetchAirportsByIcaoCodes(List<String> icaoCodes);
}
