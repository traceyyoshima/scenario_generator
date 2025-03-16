package org.base.scenario;

import org.base.scenario.model.Scenario;
import org.base.scenario.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Each number represents a time slice.
 * An elevator can only respond to a request:
 *     While idle.
 *     While moving in the same direction as the direction of the request, and while 2 floors away from the request.
 *     I.E., 1 floor away is assumed to be too jarring to the occupants.
 *     An elevator moves at 1 floor per time slice.
 * 1
 * 2
 * 3 ^ request
 * 4
 * 5 ^ elevator
 * 6
 * 7
 * 8
 * 9
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class ScenarioApplication {

    public ScenarioApplication(@Autowired ScenarioService scenarioService) {
        Scenario scenario = scenarioService.createScenario();
        scenario.generatePickupRequests();
    }

    public static void main(String[] args) {
        SpringApplication.run(ScenarioApplication.class, args);
    }
}
