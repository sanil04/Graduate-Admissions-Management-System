package com.sysc4806.gradadmissions.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/** The Resilience4j circuit breaker around notification emails (#49). */
@Configuration
public class NotificationConfig {

	/** Opens when at least half of the last 5 sends (3 or more) failed; after 60 s one trial send decides. */
	@Bean
	public CircuitBreaker emailCircuitBreaker() {
		return circuitBreaker(3, Duration.ofSeconds(60));
	}

	public static CircuitBreaker circuitBreaker(int minimumCalls, Duration waitInOpenState) {
		return CircuitBreaker.of("email", CircuitBreakerConfig.custom()
				.slidingWindowSize(5)
				.minimumNumberOfCalls(minimumCalls)
				.failureRateThreshold(50)
				.waitDurationInOpenState(waitInOpenState)
				.permittedNumberOfCallsInHalfOpenState(1)
				.build());
	}
}
