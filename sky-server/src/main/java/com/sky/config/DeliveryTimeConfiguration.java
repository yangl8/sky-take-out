package com.sky.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides an injectable clock for delivery rules and deterministic tests
 */
@Configuration
public class DeliveryTimeConfiguration {

    /**
     * Creates a system clock independent of the server default time zone
     *
     * @return UTC system clock; delivery rules convert explicitly to the configured business zone
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
