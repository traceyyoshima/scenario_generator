package org.base.scenario.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "constraints")
@Component
@Getter
@Setter
public class Constraints {
    private int numberOfFloors;
    private int numberOfRoomsPerFloor;
    private int maxOccupantsPerRoom;

    // Check-in and Check-out times are in 24-hour format to avoid confusion.
    private int checkinStartTime;
    private int checkinEndTime;
    private int checkoutStartTime;
    private int checkoutEndTime;
}
