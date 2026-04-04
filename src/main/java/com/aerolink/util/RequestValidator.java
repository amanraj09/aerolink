package com.aerolink.util;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.model.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Centralized request payload validation utility for AeroLink API.
 *
 * Handles all incoming request validation including ICAO code format,
 * count limits, and parameter presence checks.
 */
@Slf4j
@Component
public class RequestValidator {

    private static final int MAX_ICAO_CODES = 15;
    private static final Pattern ICAO_CODE_PATTERN = Pattern.compile("^[a-zA-Z]{4}$");

    /**
     * Validates request ICAO codes for airport details endpoint.
     *
     * @param icaoCodes list of ICAO codes to validate
     * @throws AeroLinkException if validation fails
     */
    public void validateAirportDetailsRequest(List<String> icaoCodes) {

        // Validation 1: At least one ICAO code required
        if (icaoCodes == null || icaoCodes.isEmpty()) {
            log.error("Validation failed — no ICAO codes provided");
            throw new AeroLinkException(ErrorCode.ICAO_CODES_REQUIRED);
        }

        // Validation 2: ICAO code count does not exceed limit
        if (icaoCodes.size() > MAX_ICAO_CODES) {
            log.error("Validation failed — {} ICAO codes exceeds limit of {}",
                    icaoCodes.size(), MAX_ICAO_CODES);
            throw new AeroLinkException(ErrorCode.ICAO_LIMIT_EXCEEDED,
                    "Maximum allowed is " + MAX_ICAO_CODES + ", but received " + icaoCodes.size());
        }

        // Validation 3: Each ICAO code must be in valid format (4 letters)
        validateIcaoCodeFormat(icaoCodes);
    }

    /**
     * Validates that each ICAO code is in the correct format.
     * Format: Exactly 4 letters
     *
     * @param icaoCodes list of ICAO codes to validate
     * @throws AeroLinkException if any code is invalid
     */
    private void validateIcaoCodeFormat(List<String> icaoCodes) {
        for (String code : icaoCodes) {
            if (code == null || !ICAO_CODE_PATTERN.matcher(code).matches()) {
                log.error("Validation failed — invalid ICAO code format: '{}'", code);
                throw new AeroLinkException(ErrorCode.ICAO_CODE_INVALID_FORMAT,
                        "Invalid ICAO code: '" + code + "'. Must be exactly 4 letters");
            }
        }
    }
}
