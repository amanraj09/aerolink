package com.aerolink.util;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.model.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestValidatorTest {

    private RequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        requestValidator = new RequestValidator();
    }

    // ─────────────────────────────────────────────
    // Rule 1: At least one ICAO code required
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 1: At least one ICAO code required")
    class AtLeastOneCode {

        @Test
        @DisplayName("throws ICAO_CODES_REQUIRED when list is null")
        void validate_nullList_throwsIcaoCodesRequired() {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(null))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_CODES_REQUIRED));
        }

        @Test
        @DisplayName("throws ICAO_CODES_REQUIRED when list is empty")
        void validate_emptyList_throwsIcaoCodesRequired() {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(Collections.emptyList()))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_CODES_REQUIRED));
        }
    }

    // ─────────────────────────────────────────────
    // Rule 2: ICAO code count limit (max 15)
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 2: ICAO code count limit (max 15)")
    class CountLimit {

        @Test
        @DisplayName("throws ICAO_LIMIT_EXCEEDED when 16 codes are provided")
        void validate_16Codes_throwsIcaoLimitExceeded() {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(generateValidCodes(16)))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("error message contains both actual count and max limit")
        void validate_16Codes_errorMessageContainsCountDetails() {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(generateValidCodes(16)))
                    .isInstanceOf(AeroLinkException.class)
                    .hasMessageContaining("16")
                    .hasMessageContaining("15");
        }

        @Test
        @DisplayName("passes with exactly 15 codes (upper boundary)")
        void validate_exactly15Codes_passes() {
            assertThatNoException().isThrownBy(
                    () -> requestValidator.validateAirportDetailsRequest(generateValidCodes(15)));
        }

        @Test
        @DisplayName("passes with exactly 1 code (lower boundary)")
        void validate_exactly1Code_passes() {
            assertThatNoException().isThrownBy(
                    () -> requestValidator.validateAirportDetailsRequest(List.of("KJFK")));
        }
    }

    // ─────────────────────────────────────────────
    // Rule 3: ICAO code format (exactly 4 letters)
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 3: ICAO code format (exactly 4 letters, case-insensitive)")
    class CodeFormat {

        @ParameterizedTest
        @ValueSource(strings = {"KJFK", "kjfk", "KjFk", "LFPG", "EGLL", "RJTT"})
        @DisplayName("passes for valid codes (uppercase, lowercase, and mixed case)")
        void validate_validFormats_passes(String code) {
            assertThatNoException().isThrownBy(
                    () -> requestValidator.validateAirportDetailsRequest(List.of(code)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"KJF", "K", "KJFKK", "KJF1", "1JFK", "KJ1K", "KJ-K", " JFK", "KJF "})
        @DisplayName("throws ICAO_CODE_INVALID_FORMAT for invalid formats")
        void validate_invalidFormats_throwsIcaoCodeInvalidFormat(String code) {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(List.of(code)))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_CODE_INVALID_FORMAT));
        }

        @Test
        @DisplayName("throws ICAO_CODE_INVALID_FORMAT when a null code is present in list")
        void validate_nullCodeInList_throwsIcaoCodeInvalidFormat() {
            List<String> codesWithNull = new ArrayList<>(List.of("KJFK"));
            codesWithNull.add(null);

            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(codesWithNull))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_CODE_INVALID_FORMAT));
        }

        @Test
        @DisplayName("error message contains the invalid code")
        void validate_invalidFormat_errorMessageContainsInvalidCode() {
            assertThatThrownBy(() -> requestValidator.validateAirportDetailsRequest(List.of("BAD1")))
                    .isInstanceOf(AeroLinkException.class)
                    .hasMessageContaining("BAD1");
        }

        @Test
        @DisplayName("fails on first invalid code when list contains mixed valid and invalid")
        void validate_mixedValidAndInvalid_failsOnFirstInvalidCode() {
            assertThatThrownBy(() ->
                    requestValidator.validateAirportDetailsRequest(List.of("KJFK", "BAD1", "LFPG")))
                    .isInstanceOf(AeroLinkException.class)
                    .satisfies(ex -> {
                        assertErrorCode((AeroLinkException) ex, ErrorCode.ICAO_CODE_INVALID_FORMAT);
                        assertThat(ex.getMessage()).contains("BAD1");
                    });
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    /** Generates n valid 4-uppercase-letter ICAO-like codes: KAAA, KAAB, KAAC, ... */
    private List<String> generateValidCodes(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> String.format("K%c%c%c",
                        (char) ('A' + (i / 676) % 26),
                        (char) ('A' + (i / 26) % 26),
                        (char) ('A' + i % 26)))
                .toList();
    }

    private void assertErrorCode(AeroLinkException ex, ErrorCode expected) {
        assertThat(ex.getErrorCode()).isEqualTo(expected);
    }
}
