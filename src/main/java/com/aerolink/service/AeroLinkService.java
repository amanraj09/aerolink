package com.aerolink.service;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.config.AviationDataProviderRegistry;
import com.aerolink.model.response.AirportDetail;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service layer containing business logic for retrieving and transforming airport data.
 *
 * <p>Acts as the intermediary between the controller and the external aviation data provider.
 * Delegates the upstream API call to {@link AviationDataProvider}, keeping the service decoupled
 * from any specific provider implementation.
 */
@Slf4j
@Service
public class AeroLinkService {

  private final AviationDataProviderRegistry aviationDataProviderRegistry;

  @Value("${aerolink.provider}")
  private String activeProvider;

  public AeroLinkService(AviationDataProviderRegistry aviationDataProviderRegistry) {
    this.aviationDataProviderRegistry = aviationDataProviderRegistry;
  }

  /**
   * Retrieves airport details for the given list of ICAO codes.
   *
   * @param icaoCodes list of ICAO airport identifiers (case-insensitive)
   * @return list of {@link AirportDetail} objects
   */
  public List<AirportDetail> getAirportDetails(List<String> icaoCodes) {
    List<String> normalizedCodes = normalizeIcaoCodes(icaoCodes);
    log.info(
        "Fetching airport details for {} ICAO code(s): {}",
        normalizedCodes.size(),
        normalizedCodes);
    return aviationDataProviderRegistry
        .getActiveProviderByName(activeProvider)
        .fetchAirportsByIcaoCodes(normalizedCodes);
  }

  /**
   * Normalizes ICAO codes to uppercase. Ensures consistent format before passing to the upstream
   * API,
   *
   * @param icaoCodes raw ICAO codes from the request
   * @return list of uppercased ICAO codes
   */
  private List<String> normalizeIcaoCodes(List<String> icaoCodes) {
    return icaoCodes.stream().map(String::toUpperCase).toList();
  }
}
