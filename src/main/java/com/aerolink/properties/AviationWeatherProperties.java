package com.aerolink.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalises all Aviation Weather API client configuration.
 *
 * Decision: Using @ConfigurationProperties over @Value for structured config.
 * Groups all related settings together, enables validation, and keeps the
 * service 12-factor compliant — all config comes from the environment.
 *
 * @param baseUrl        Base URL of the Aviation Weather API
 * @param connectTimeout Maximum time to wait when establishing a connection
 * @param readTimeout    Maximum time to wait for a response once connected
 */
@ConfigurationProperties(prefix = "aviation.weather.api")
public record AviationWeatherProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
