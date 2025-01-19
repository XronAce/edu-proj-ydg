package com.ktds.eduuser.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        log.debug("Health check called.");
        return Health.up().build();
    }
}
