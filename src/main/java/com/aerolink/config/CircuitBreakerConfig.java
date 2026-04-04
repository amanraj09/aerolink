package com.aerolink.config;

import com.aerolink.constant.AeroLinkConstants;
import com.aerolink.exception.AeroLinkException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Configures the CircuitBreaker for upstream Aviation Weather API calls.
 *
 */
@Configuration
public class CircuitBreakerConfig {

    private static final int    SLIDING_WINDOW_SIZE         = 10;
    private static final float  FAILURE_RATE_THRESHOLD      = 50.0f;
    private static final int    WAIT_DURATION_OPEN_SECONDS  = 10;
    private static final int    HALF_OPEN_PERMITTED_CALLS   = 3;

    @Bean(name = AeroLinkConstants.AVIATION_WEATHER_CLIENT_CIRCUIT_BREAKER)
    public CircuitBreaker aviationWeatherCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(SLIDING_WINDOW_SIZE)
                        .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                        .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_OPEN_SECONDS))
                        .permittedNumberOfCallsInHalfOpenState(HALF_OPEN_PERMITTED_CALLS)
                        .recordExceptions(RestClientException.class)
                        .ignoreExceptions(AeroLinkException.class)
                        .build();

        return CircuitBreaker.of("aviationWeather", config);
    }
}
