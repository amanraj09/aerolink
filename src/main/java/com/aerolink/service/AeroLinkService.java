package com.aerolink.service;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.config.AviationDataProviderRegistry;
import com.aerolink.exception.AeroLinkException;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.response.AirportDetail;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service layer containing business logic for retrieving and transforming airport data. */
@Slf4j
@Service
public class AeroLinkService {

  private final AviationDataProvider aviationDataProvider;
  private final AeroLinkMetrics metrics;

  public AeroLinkService(
      AviationDataProviderRegistry aviationDataProviderRegistry,
      @Value("${aerolink.provider}") String activeProvider,
      AeroLinkMetrics metrics) {
    this.aviationDataProvider =
        aviationDataProviderRegistry.getActiveProviderByName(activeProvider);
    this.metrics = metrics;
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

    metrics.recordIcaoCodesRequested(normalizedCodes.size());
    Timer.Sample sample = metrics.startLookupTimer();

    try {
      List<AirportDetail> results = aviationDataProvider.fetchAirportsByIcaoCodes(normalizedCodes);

      String outcome = results.isEmpty() ? "empty" : "success";
      metrics.recordLookupRequest(outcome);
      metrics.recordAirportsReturned(results.size());
      metrics.stopLookupTimer(sample, outcome);
      return results;
    } catch (AeroLinkException ex) {
      metrics.recordLookupRequest("error");
      metrics.stopLookupTimer(sample, "error");
      throw ex;
    }
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
