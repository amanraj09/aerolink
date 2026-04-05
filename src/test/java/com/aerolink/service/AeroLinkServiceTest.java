package com.aerolink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.config.AviationDataProviderRegistry;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AeroLinkServiceTest {

  private static final String ACTIVE_PROVIDER = "aviationWeather";

  @Mock private AviationDataProviderRegistry aviationDataProviderRegistry;
  @Mock private AviationDataProvider aviationDataProvider;
  @Mock private AeroLinkMetrics metrics;

  private AeroLinkService aeroLinkService;

  @BeforeEach
  void setUp() {
    when(aviationDataProviderRegistry.getActiveProviderByName(ACTIVE_PROVIDER))
        .thenReturn(aviationDataProvider);
    when(metrics.startLookupTimer()).thenReturn(Timer.start());
    aeroLinkService = new AeroLinkService(aviationDataProviderRegistry, ACTIVE_PROVIDER, metrics);
  }

  // ─────────────────────────────────────────────
  // ICAO code normalization
  // ─────────────────────────────────────────────

  @Nested
  class Normalization {

    @Test
    void getAirportDetails_lowercaseCodes_normalizedToUppercase() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG")))
          .thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("kjfk", "lfpg"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG"));
    }

    @Test
    void getAirportDetails_mixedCaseCodes_normalizedToUppercase() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK"))).thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("KjFk"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK"));
    }

    @Test
    void getAirportDetails_uppercaseCodes_passedUnchanged() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG")))
          .thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("KJFK", "LFPG"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG"));
    }

    @Test
    void getAirportDetails_multipleCodesVariousCases_allNormalized() {
      List<String> expected = List.of("KJFK", "LFPG", "EGLL");
      when(aviationDataProvider.fetchAirportsByIcaoCodes(expected)).thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("kjfk", "LFPG", "EgLl"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(expected);
    }
  }

  // ─────────────────────────────────────────────
  // Return value from provider
  // ─────────────────────────────────────────────

  @Nested
  class ReturnValue {

    @Test
    void getAirportDetails_returnsProviderResult() {
      AirportDetail mockDetail = buildMockAirportDetail("KJFK", "John F Kennedy Intl");
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .thenReturn(List.of(mockDetail));

      List<AirportDetail> result = aeroLinkService.getAirportDetails(List.of("kjfk"));

      assertThat(result).containsExactly(mockDetail);
    }

    @Test
    void getAirportDetails_noMatchFound_returnsEmptyList() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("ZZZZ"))).thenReturn(List.of());

      List<AirportDetail> result = aeroLinkService.getAirportDetails(List.of("zzzz"));

      assertThat(result).isEmpty();
    }
  }

  // ─────────────────────────────────────────────
  // Helper
  // ─────────────────────────────────────────────

  private AirportDetail buildMockAirportDetail(String icaoCode, String name) {
    return new AirportDetail(
        name, new AirportIdentifier(icaoCode, null, null), null, null, null, List.of());
  }
}
