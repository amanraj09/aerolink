package com.aerolink.config;

import com.aerolink.exception.AeroLinkException;
import com.aerolink.constant.AeroLinkConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Configures the RetryTemplate for upstream API calls.
 * Retry spec:
 *   - Max attempts : 3 (1 original + 2 retries)
 *   - Backoff      : exponential starting at 200ms, multiplier 2
 *                    attempt 1 fails → wait 200ms
 *                    attempt 2 fails → wait 400ms
 *                    attempt 3 fails → throw AeroLinkException
 *   - Retry on     : RestClientException (network/server errors)
 */
@Configuration
public class RetryConfig {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @Bean(name = AeroLinkConstants.AVIATION_WEATHER_CLIENT_RETRY)
    public RetryTemplate retryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_MS);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_ATTEMPTS,
                Map.of(
                        RestClientException.class, true
                ));

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);
        return template;
    }
}
