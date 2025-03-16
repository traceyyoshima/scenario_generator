package org.base.scenario.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Room {
    private final int roomNumber;

    @Setter
    int occupants;

    public Room(int roomNumber) {
        this.roomNumber = roomNumber;
    }
}
