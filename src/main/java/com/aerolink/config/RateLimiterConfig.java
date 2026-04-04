package com.aerolink.config;

import com.aerolink.constant.AeroLinkConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the rate limiter bucket for incoming API requests.
 *
 * Using Bucket4j's token bucket algorithm — each request consumes one token,
 * and 60 tokens are refilled every minute. Set way below the upstream limit of 100/min
 * to provide a safety buffer and prevent AeroLink from exhausting the upstream quota.
 */
@Configuration
public class RateLimiterConfig {

    private static final int REQUEST_LIMIT_PER_MINUTE = 60;
    private static final int TOKEN_REFILL_RATE = 1;

    @Bean(name = AeroLinkConstants.AVIATION_WEATHER_CLIENT_RATE_LIMITER)
    public Bucket rateLimiterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUEST_LIMIT_PER_MINUTE)
                .refillGreedy(TOKEN_REFILL_RATE, Duration.ofSeconds(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
