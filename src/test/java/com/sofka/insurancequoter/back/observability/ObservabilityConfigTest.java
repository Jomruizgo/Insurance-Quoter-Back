package com.sofka.insurancequoter.back.observability;

import com.sofka.insurancequoter.InsuranceQuoterApplication;
import io.micrometer.observation.aop.ObservedAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InsuranceQuoterApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ObservabilityConfigTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ApplicationContext context;

    @Test
    void observedAspect_bean_exists_in_context() {
        assertThat(context.getBean(ObservedAspect.class)).isNotNull();
    }
}
