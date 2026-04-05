package com.aerolink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.config.AviationDataProviderRegistry;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AeroLinkServiceTest {

  private static final String ACTIVE_PROVIDER = "aviationWeather";

  @Mock private AviationDataProviderRegistry aviationDataProviderRegistry;
  @Mock private AviationDataProvider aviationDataProvider;

  @InjectMocks private AeroLinkService aeroLinkService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(aeroLinkService, "activeProvider", ACTIVE_PROVIDER);
    when(aviationDataProviderRegistry.getActiveProviderByName(ACTIVE_PROVIDER))
        .thenReturn(aviationDataProvider);
  }

  // ─────────────────────────────────────────────
  // ICAO code normalization
  // ─────────────────────────────────────────────

  @Nested
  @DisplayName("ICAO code normalization to uppercase")
  class Normalization {

    @Test
    @DisplayName("normalizes lowercase codes to uppercase before upstream call")
    void getAirportDetails_lowercaseCodes_normalizedToUppercase() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG")))
          .thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("kjfk", "lfpg"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG"));
    }

    @Test
    @DisplayName("normalizes mixed case codes to uppercase before upstream call")
    void getAirportDetails_mixedCaseCodes_normalizedToUppercase() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK"))).thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("KjFk"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK"));
    }

    @Test
    @DisplayName("passes already uppercase codes to upstream unchanged")
    void getAirportDetails_uppercaseCodes_passedUnchanged() {
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG")))
          .thenReturn(List.of());

      aeroLinkService.getAirportDetails(List.of("KJFK", "LFPG"));

      verify(aviationDataProvider).fetchAirportsByIcaoCodes(List.of("KJFK", "LFPG"));
    }

    @Test
    @DisplayName("normalizes all codes in a multi-code list with various casing")
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
  @DisplayName("Return value from provider")
  class ReturnValue {

    @Test
    @DisplayName("returns airport details received from upstream provider")
    void getAirportDetails_returnsProviderResult() {
      AirportDetail mockDetail = buildMockAirportDetail("KJFK", "John F Kennedy Intl");
      when(aviationDataProvider.fetchAirportsByIcaoCodes(List.of("KJFK")))
          .thenReturn(List.of(mockDetail));

      List<AirportDetail> result = aeroLinkService.getAirportDetails(List.of("kjfk"));

      assertThat(result).containsExactly(mockDetail);
    }

    @Test
    @DisplayName("returns empty list when provider finds no matches")
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
