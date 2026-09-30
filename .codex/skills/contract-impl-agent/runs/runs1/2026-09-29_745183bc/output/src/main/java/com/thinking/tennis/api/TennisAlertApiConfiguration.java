package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import com.thinking.tennis.app.AvailabilityApplicationService;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TennisAlertApiConfiguration {

    @Bean
    Clock tennisAlertClock() {
        return Clock.systemUTC();
    }

    @Bean
    AvailabilityApplicationService availabilityApplicationService(CourtAvailabilityPort courtAvailabilityPort,
                                                                 Clock tennisAlertClock) {
        return new AvailabilityApplicationService(courtAvailabilityPort, tennisAlertClock);
    }

    @Bean
    AlertApplicationService alertApplicationService(AvailabilityApplicationService availabilityApplicationService,
                                                   CourtAvailabilityPort courtAvailabilityPort,
                                                   Clock tennisAlertClock) {
        return new AlertApplicationService(availabilityApplicationService, courtAvailabilityPort, tennisAlertClock);
    }
}
