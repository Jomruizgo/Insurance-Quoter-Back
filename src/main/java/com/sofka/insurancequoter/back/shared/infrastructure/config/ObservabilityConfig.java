package com.sofka.insurancequoter.back.shared.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Enables @Observed AOP support.
// OTel OTLP export is auto-configured by spring-boot-starter-opentelemetry via
// management.opentelemetry.tracing.export.otlp.endpoint — no manual SpanExporter bean needed.
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
