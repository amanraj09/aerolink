package com.aerolink.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.metrics.AeroLinkMetrics;
import com.aerolink.model.error.ErrorCode;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import com.aerolink.service.AeroLinkService;
import com.aerolink.util.RequestValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link AeroLinkController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (controller + exception handler). {@link
 * RequestValidator} is imported directly since {@code @WebMvcTest} does not auto-scan
 * {@code @Component} beans outside the web layer. {@link AeroLinkService} is mocked via
 * {@code @MockitoBean}.
 */
@WebMvcTest(AeroLinkController.class)
@Import(RequestValidator.class)
class AeroLinkControllerTest {

  private static final String BASE_URL = "/api/v1/airport";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AeroLinkService aeroLinkService;
  @MockitoBean private AeroLinkMetrics metrics;

  // ─────────────────────────────────────────────
  // Happy Path
  // ─────────────────────────────────────────────

  @Test
  void getAirportDetails_singleIcaoCode_returns200() throws Exception {
    AirportDetail detail = buildMockAirportDetail("KJFK", "John F Kennedy Intl");
    when(aeroLinkService.getAirportDetails(List.of("KJFK"))).thenReturn(List.of(detail));

    mockMvc
        .perform(get(BASE_URL).param("icaoCodes", "KJFK").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].airportName").value("John F Kennedy Intl"))
        .andExpect(jsonPath("$[0].identifier.icaoId").value("KJFK"));

    verify(aeroLinkService, times(1)).getAirportDetails(List.of("KJFK"));
  }

  @Test
  void getAirportDetails_multipleIcaoCodes_returns200() throws Exception {
    List<String> icaoCodes = List.of("KJFK", "KLAX", "KORD");
    List<AirportDetail> details =
        List.of(
            buildMockAirportDetail("KJFK", "John F Kennedy Intl"),
            buildMockAirportDetail("KLAX", "Los Angeles Intl"),
            buildMockAirportDetail("KORD", "Chicago O'Hare Intl"));
    when(aeroLinkService.getAirportDetails(icaoCodes)).thenReturn(details);

    mockMvc
        .perform(
            get(BASE_URL)
                .param("icaoCodes", "KJFK", "KLAX", "KORD")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].airportName").value("John F Kennedy Intl"))
        .andExpect(jsonPath("$[1].airportName").value("Los Angeles Intl"))
        .andExpect(jsonPath("$[2].airportName").value("Chicago O'Hare Intl"));

    verify(aeroLinkService, times(1)).getAirportDetails(icaoCodes);
  }

  @Test
  void getAirportDetails_exactly15IcaoCodes_returns200() throws Exception {
    List<String> icaoCodes =
        List.of(
            "KAAA", "KAAB", "KAAC", "KAAD", "KAAE", "KAAF", "KAAG", "KAAH", "KAAI", "KAAJ", "KAAK",
            "KAAL", "KAAM", "KAAN", "KAAO");
    when(aeroLinkService.getAirportDetails(icaoCodes)).thenReturn(List.of());

    mockMvc
        .perform(
            get(BASE_URL)
                .param(
                    "icaoCodes",
                    "KAAA",
                    "KAAB",
                    "KAAC",
                    "KAAD",
                    "KAAE",
                    "KAAF",
                    "KAAG",
                    "KAAH",
                    "KAAI",
                    "KAAJ",
                    "KAAK",
                    "KAAL",
                    "KAAM",
                    "KAAN",
                    "KAAO")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(aeroLinkService, times(1)).getAirportDetails(icaoCodes);
  }

  @Test
  void getAirportDetails_noMatchesFound_returns200WithEmptyList() throws Exception {
    when(aeroLinkService.getAirportDetails(List.of("XXXX"))).thenReturn(List.of());

    mockMvc
        .perform(get(BASE_URL).param("icaoCodes", "XXXX").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ─────────────────────────────────────────────
  // Validation / Error Cases
  // ─────────────────────────────────────────────

  @Test
  void getAirportDetails_over15IcaoCodes_returns400() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .param(
                    "icaoCodes",
                    "KAAA",
                    "KAAB",
                    "KAAC",
                    "KAAD",
                    "KAAE",
                    "KAAF",
                    "KAAG",
                    "KAAH",
                    "KAAI",
                    "KAAJ",
                    "KAAK",
                    "KAAL",
                    "KAAM",
                    "KAAN",
                    "KAAO",
                    "KAAP")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AERO-102"));

    verifyNoInteractions(aeroLinkService);
  }

  @Test
  void getAirportDetails_over15IcaoCodes_returnsCorrectErrorBody() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .param(
                    "icaoCodes",
                    "KAAA",
                    "KAAB",
                    "KAAC",
                    "KAAD",
                    "KAAE",
                    "KAAF",
                    "KAAG",
                    "KAAH",
                    "KAAI",
                    "KAAJ",
                    "KAAK",
                    "KAAL",
                    "KAAM",
                    "KAAN",
                    "KAAO",
                    "KAAP")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AERO-102"))
        .andExpect(jsonPath("$.message").value("Maximum allowed is 15, but received 16"));
  }

  @Test
  void getAirportDetails_invalidIcaoFormat_returns400() throws Exception {
    mockMvc
        .perform(get(BASE_URL).param("icaoCodes", "KJF1").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AERO-103"))
        .andExpect(
            jsonPath("$.message").value("Invalid ICAO code: 'KJF1'. Must be exactly 4 letters"));

    verifyNoInteractions(aeroLinkService);
  }

  @Test
  void getAirportDetails_rateLimitExceeded_returns429() throws Exception {
    when(aeroLinkService.getAirportDetails(any()))
        .thenThrow(new AeroLinkException(ErrorCode.UPSTREAM_RATE_LIMIT_EXCEEDED));

    mockMvc
        .perform(get(BASE_URL).param("icaoCodes", "KJFK").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.errorCode").value("AERO-104"));
  }

  // ─────────────────────────────────────────────
  // Helper
  // ─────────────────────────────────────────────

  private AirportDetail buildMockAirportDetail(String icaoCode, String name) {
    return new AirportDetail(
        name, new AirportIdentifier(icaoCode, null, null), null, null, null, List.of());
  }
}
