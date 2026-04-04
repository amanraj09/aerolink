package com.aerolink.config;

import com.aerolink.client.AviationDataProvider;
import com.aerolink.client.aviationweather.AviationWeatherClient;
import com.aerolink.constant.AeroLinkConstants;
import com.aerolink.exception.AeroLinkException;
import com.aerolink.properties.AviationProviderProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties(AviationProviderProperties.class)
public class AeroLinkConfiguration {

    // ─── RestClient ───────────────────────────────────────────────────────────

    @Bean(name = AeroLinkConstants.PROVIDER_REST_CLIENT)
    public RestClient restClient(AviationProviderProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(buildRequestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private HttpComponentsClientHttpRequestFactory buildRequestFactory(AviationProviderProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.maxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(properties.maxConnectionsPerRoute());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(properties.connectTimeout()))
                .setResponseTimeout(Timeout.of(properties.readTimeout()))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    // ─── Retry ────────────────────────────────────────────────────────────────

    private static final int    MAX_ATTEMPTS       = 3;
    private static final long   INITIAL_BACKOFF_MS = 200;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @Bean(name = AeroLinkConstants.PROVIDER_RETRY)
    public RetryTemplate retryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_MS);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_ATTEMPTS,
                Map.of(RestClientException.class, true));

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);
        return template;
    }

    // ─── Circuit Breaker ──────────────────────────────────────────────────────

    private static final int   SLIDING_WINDOW_SIZE        = 10;
    private static final float FAILURE_RATE_THRESHOLD     = 50.0f;
    private static final int   WAIT_DURATION_OPEN_SECONDS = 10;
    private static final int   HALF_OPEN_PERMITTED_CALLS  = 3;

    @Bean(name = AeroLinkConstants.PROVIDER_CIRCUIT_BREAKER)
    public CircuitBreaker circuitBreaker() {
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

    // ─── Rate Limiter ─────────────────────────────────────────────────────────

    private static final int TOKEN_REFILL_RATE = 1;

    @Bean(name = AeroLinkConstants.PROVIDER_RATE_LIMITER)
    public Bucket rateLimiterBucket(AviationProviderProperties properties) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.requestLimitPerMinute())
                .refillGreedy(TOKEN_REFILL_RATE, Duration.ofSeconds(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // ─── Client ───────────────────────────────────────────────────────────────

    @Bean
    public AviationDataProvider aviationWeatherClient(RestClient restClient, Bucket rateLimiterBucket,
                                                      RetryTemplate retryTemplate, CircuitBreaker circuitBreaker) {
        return new AviationWeatherClient(restClient, rateLimiterBucket, retryTemplate, circuitBreaker);
    }
}
