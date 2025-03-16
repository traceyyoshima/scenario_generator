package org.base.scenario.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Getter
public class Building {
    private final Constraints constraints;
    private final Map<Integer, Floor> floors = new TreeMap<>();
    private int currentFloor = 2; // Starts from floor 2 to force the elevator to go up.

    public Building(Constraints constraints) {
        this.constraints = constraints;
        int currentRoomNumber = 1;
        for (int i = 1; i <= constraints.getNumberOfFloors(); i++) {
            Floor floor = new Floor(i);
            for (int j = 1; j <= constraints.getNumberOfRoomsPerFloor(); j++) {
                Room room = new Room(currentRoomNumber);
                floor.addRoom(room);
                currentRoomNumber++;
            }
            floors.put(i, floor);
        }
    }

    public Floor getNextFloorWithRoom() {
        for (int i = currentFloor; i <= constraints.getNumberOfFloors(); i++) {
            Floor floor = floors.get(i);
            if (floor.getAvailableRoom() != null) {
                return floor;
            }
            currentFloor++;
        }
        return null;
    }

    public void checkInOccupants(Floor floor, int occupants) {
        floor.checkInOccupants(occupants);
    }

    public List<Room> getOccupiedRooms() {
        List<Room> occupiedRooms = new ArrayList<>();
        for (Floor floor : floors.values()) {
            for (Room room : floor.getRoomList()) {
                if (room.getOccupants() > 0) {
                    occupiedRooms.add(room);
                }
            }
        }
        return occupiedRooms;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Floor floor : floors.values()) {
            sb.append("Floor " )
                    .append(floor.getFloorNumber())
                    .append(":")
                    .append(System.lineSeparator());
            for (Room room : floor.getRoomList()) {
                sb.append("    Room ")
                        .append(room.getRoomNumber())
                        .append(", Occupants: ")
                        .append(room.getOccupants())
                        .append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
