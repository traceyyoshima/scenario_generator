package org.base.scenario.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Scenario {

    private final Map<Integer, List<MoveRequest>> pickupRequests;

    public Scenario(Map<Integer, List<MoveRequest>> pickupRequests) {
        this.pickupRequests = pickupRequests;
    }

    public void generatePickupRequests() {
        // TODO: names could be set via properties and generates by env name.
        //   #---
        //     spring env name.
        String name = "scenario";
        try (PrintWriter writer = new PrintWriter(name + ".txt", StandardCharsets.UTF_8)) {
            for (Map.Entry<Integer, List<MoveRequest>> entry : pickupRequests.entrySet()) {
                Integer timeSlice = entry.getKey();
                List<MoveRequest> requests = entry.getValue();
                for (MoveRequest request : requests) {
                    writer.println(String.format("%s, %s, %s", timeSlice, request.getCurrentFloor(), request.getDestinationFloor()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
    }
}
