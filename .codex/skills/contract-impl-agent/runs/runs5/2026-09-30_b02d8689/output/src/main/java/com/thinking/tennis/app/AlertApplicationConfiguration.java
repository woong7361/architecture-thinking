package com.thinking.tennis.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertApplicationConfiguration {

    @Bean
    AlertRepository alertRepository() {
        return new InMemoryAlertRepository();
    }

    @Bean
    AvailabilityRepository availabilityRepository() {
        return new InMemoryAvailabilityRepository();
    }
}
