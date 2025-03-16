package org.base.scenario.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a pick-up request and an elevator request to a target floor.
 */
@AllArgsConstructor
@Data
public class MoveRequest {
    // TODO: it may be interesting to save the roomId to visualize room movement.
    //   It is current possible for more occupants to leave a floor in code, which is unrealistic.
    int currentFloor;
    int destinationFloor;

    @Nullable
    Integer targetTime;

    public MoveRequest(int currentFloor, int destinationFloor) {
        this.currentFloor = currentFloor;
        this.destinationFloor = destinationFloor;
        this.targetTime = null;
    }

    public void validate() {
        if (currentFloor == destinationFloor) {
            throw new IllegalArgumentException("Current floor and destination floor cannot be the same");
        }
    }
}
