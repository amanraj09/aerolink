package com.aerolink.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Externalises all aviation provider client configuration.
 * @param baseUrl                Base URL of the aviation provider API
 * @param connectTimeout         Maximum time to wait when establishing a connection
 * @param readTimeout            Maximum time to wait for a response once connected
 * @param maxTotalConnections    Maximum total connections in the HTTP connection pool
 * @param maxConnectionsPerRoute Maximum connections per route in the HTTP connection pool
 * @param requestLimitPerMinute  Maximum requests per minute sent to the upstream API
 */
@ConfigurationProperties(prefix = "aviation.provider.api")
public record AviationProviderProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxTotalConnections,
        int maxConnectionsPerRoute,
        int requestLimitPerMinute
) {
}
