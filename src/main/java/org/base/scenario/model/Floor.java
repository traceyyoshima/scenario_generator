package org.base.scenario.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Floor {

    private final int floorNumber;
    private final List<Room> roomList = new ArrayList<>();
    private int currentIndex = 0;

    public Floor(int roomsPerFloor) {
        this.floorNumber = roomsPerFloor;
    }

    public void addRoom(Room room) {
        this.roomList.add(room);
    }

    public Room getAvailableRoom() {
        if (currentIndex < roomList.size()) {
            return roomList.get(currentIndex);
        }
        return null;
    }

    public void checkInOccupants(int occupants) {
        Room room = getAvailableRoom();
        if (room != null) {
            room.setOccupants(occupants);
            currentIndex++;
        }
    }
}
