package com.aerolink.client.aviationweather;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.client.model.AviationWeatherRawResponse;
import com.aerolink.model.response.AirportCommunications;
import com.aerolink.model.response.AirportDetail;
import com.aerolink.model.response.AirportIdentifier;
import com.aerolink.model.response.AirportLocation;
import com.aerolink.model.response.AirportOperations;
import com.aerolink.model.response.RunwayDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

/**
 * RestClient-based implementation of {@link AviationDataProvider} for public Aviation Weather API.
 */
@Slf4j
@Component
public class AviationWeatherClient implements AviationDataProvider {

    private static final String AIRPORT_PATH = "/airport";

    private final RestClient restClient;

    public AviationWeatherClient(RestClient aviationWeatherRestClient) {
        this.restClient = aviationWeatherRestClient;
    }

    /**
     * Fetches airport details for the given ICAO codes in a single upstream API call.
     *
     * @param icaoCodes list of ICAO codes to look up
     * @return list of mapped {@link AirportDetail} objects
     */
    @Override
    public List<AirportDetail> fetchAirportsByIcaoCodes(List<String> icaoCodes) {
        String ids = String.join(",", icaoCodes);
        log.info("Calling Aviation Weather API for IDs: {}", ids);

        List<AviationWeatherRawResponse> rawResponses = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(AIRPORT_PATH)
                        .queryParam("format", "json")
                        .queryParam("ids", ids)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (CollectionUtils.isEmpty(rawResponses)) {
            log.warn("Aviation Weather API returned empty response for IDs: {}", ids);
            return List.of();
        }

        log.info("Aviation Weather API returned {} result(s) for IDs: {}", rawResponses.size(), ids);
        return rawResponses.stream()
                .map(this::mapToAirportDetail)
                .toList();
    }

    /**
     * Maps a raw upstream API response to  provider-agnostic {@link AirportDetail}.
     *
     * Mapping  here in the client layer so the service layer remains client schema agnostic.
     * If the API schema changes, only this method needs updating.
     *
     * @param response response from the upstream API
     * @return mapped AirportDetail
     */
    private AirportDetail mapToAirportDetail(AviationWeatherRawResponse response) {
        List<RunwayDetail> runways = response.runways() == null ? List.of() :
                response.runways().stream()
                        .map(r -> new RunwayDetail(r.id(), r.dimension(), r.surface(), r.alignment()))
                        .toList();

        return new AirportDetail(
                response.name() != null ? response.name().trim() : null,
                new AirportIdentifier(response.icaoId(), response.iataId(), response.faaId()),
                new AirportLocation(response.state(), response.country(), response.lat(), response.lon(), response.elev()),
                new AirportOperations(response.owner(), response.tower(), response.beacon(), response.services(), response.operations(), response.passengers()),
                new AirportCommunications(response.freqs(), response.magdec()),
                runways
        );
    }
}
