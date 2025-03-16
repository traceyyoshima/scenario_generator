package org.base.scenario.service;

import org.base.scenario.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScenarioService {

    private final Constraints constraints;
    private final Building building;
    private final Set<Integer> timeSlices = new HashSet<>(); // This is a hack to process request in order regardless of time value.

    public ScenarioService(@Autowired Constraints constraints) {
        System.out.println("Generating scenario...");
        this.constraints = constraints;
        this.building = new Building(constraints);
    }

    public Scenario createScenario() {
        Map<Integer, List<MoveRequest>> pickupRequests = generateCheckinPickupRequests();
        randomizeCheckinDestinations(pickupRequests);
        Queue<MoveRequest> returns = addOccupantLeaveRequests(pickupRequests);
        addReturnRequests(pickupRequests, returns);
        generateCheckoutPickupRequests(pickupRequests);
        // TODO: maybe randomize checkouts and add checkins 1 hr after checkout.
        printPickupRequests(pickupRequests);
        normalizePickupRequestsPerHour(pickupRequests);
        return new Scenario(pickupRequests);
    }

    /**
     * Generate pickup requests between checkin start time and checkin end time.
     * @return Map of time slice to list of pickup requests.
     */
    private Map<Integer, List<MoveRequest>> generateCheckinPickupRequests() {
        Map<Integer, List<MoveRequest>> pickupRequests = new TreeMap<>();

        // TODO: Maybe move target occupancy to property file.
        //    Occupancy could be refactored to allow for a queue of checkins after the building is full.
        //    A queue would show the benefit of increasing the floors or rooms per floor.
        int targetOccupancy = (int) Math.round(constraints.getNumberOfFloors() * constraints.getNumberOfRoomsPerFloor() * .8);

        // TODO: Maybe move % of checkins during prime time to property file.
        //    40% of the checkins will happen during prime time (15:00 - 17:00)
        int checkinsDuringPrimeTime = (int) Math.round(targetOccupancy * .4);
        // This could be slightly randomized for more interesting data.
        int checkinsPerPrimeTimeHour = Math.round((float) checkinsDuringPrimeTime / 3);

        int remainingCheckins = targetOccupancy - checkinsDuringPrimeTime;
        // This could be slightly randomized for more interesting data.
        int checkinsPerNonPrimeTimeHour = Math.round((float) remainingCheckins / (constraints.getCheckinEndTime() - constraints.getCheckinStartTime() - 3));

        Random rand = new Random(System.currentTimeMillis());
        for (int i = constraints.getCheckinStartTime(); i <= constraints.getCheckinEndTime(); i++) {
            int occupants = rand.nextInt(4) + 1;
            timeSlices.add(i);
            Floor floor = building.getNextFloorWithRoom();
            if (floor == null) {
                break;
            }

            // TODO: Maybe move prime time range to property file.
            boolean isBusyTime = i == 15 || i == 16 || i == 17;
            int checkins = isBusyTime ? checkinsPerPrimeTimeHour : checkinsPerNonPrimeTimeHour;
            for (int j = 0; j < checkins; j++) {
                MoveRequest moveRequest = new MoveRequest(1, floor.getFloorNumber());
                moveRequest.validate();
                pickupRequests.computeIfAbsent(timeSlices.size(), _ -> new ArrayList<>()).add(moveRequest);
                building.checkInOccupants(floor, occupants);
            }
        }

        return pickupRequests;
    }

    /**
     * The intent of this method is to create more interesting scenario data.
     * Randomly swaps the destination floors of pickup requests.
     */
    private void randomizeCheckinDestinations(Map<Integer, List<MoveRequest>> pickupRequests) {
        List<Integer> keys = new ArrayList<>(pickupRequests.size());
        Map<Integer, List<Integer>> indexMap = new HashMap<>();
        for (Map.Entry<Integer, List<MoveRequest>> integerListEntry : pickupRequests.entrySet()) {
            keys.add(integerListEntry.getKey());
            List<MoveRequest> value = integerListEntry.getValue();
            for (int i = 0; i < value.size(); i++) {
                indexMap.computeIfAbsent(integerListEntry.getKey(), _ -> new ArrayList<>()).add(i);
            }
        }

        Random rand = new Random(System.currentTimeMillis());
        while (!keys.isEmpty()) {
            int keyIndex = rand.nextInt(keys.size());
            int key = keys.remove(keyIndex);
            if (keys.isEmpty()) {
                break;
            }
            List<Integer> prIndexes = indexMap.get(key);
            for (int i = 0; i < prIndexes.size(); i++) {
                MoveRequest pr1 = pickupRequests.get(key).get(i);

                // Randomly select a pickup request from another time slice.
                int keyIndex2 = rand.nextInt(keys.size());
                int key2 = keys.get(keyIndex2);
                List<Integer> indexes2 = indexMap.get(key2);
                int prIndex2 = rand.nextInt(indexes2.size());
                MoveRequest pr2 = pickupRequests.get(key2).get(prIndex2);

                // Swap the destination floors.
                int destFloor = pr1.getDestinationFloor();
                pr1.setDestinationFloor(pr2.getDestinationFloor());
                pr2.setDestinationFloor(destFloor);
            }
        }
    }

    /**
     * The intent of this method is to create more interesting scenario data.
     * Occupants will leave the building during the day, and come back.
     */
    private Queue<MoveRequest> addOccupantLeaveRequests(Map<Integer, List<MoveRequest>> pickupRequests) {
        // It seems reasonable for occupants to use the elevator more often between certain times, like lunch, dinner, or an
        // evening out.
        // For more interesting data, a percentage of occupants can move from 1 floor to another instead of leaving the building.
        // Must return by 12PM.
        // Every occupant that leaves needs to return before checkout time.
        Random rand = new Random(System.currentTimeMillis());
        Queue<MoveRequest> leaveQueue = new LinkedList<>();
        Queue<MoveRequest> returnQueue = new LinkedList<>();
        for (Integer i : pickupRequests.keySet()) {
            int portionOfOccupants = rand.nextInt(20) + 20;
            List<MoveRequest> moveRequestList = pickupRequests.get(i);
            int numberOfUpdates = (int) Math.round(moveRequestList.size() * portionOfOccupants * .01);
            Set<Integer> updatedIndexes = new HashSet<>();
            for (int j = 0; j < numberOfUpdates; j++) {
                int index = rand.nextInt(moveRequestList.size());
                if (!updatedIndexes.add(index) || !pickupRequests.containsKey(i + 1)) {
                    continue;
                }
                MoveRequest moveRequest = moveRequestList.get(index);
                Elevator.Direction direction;
                if (moveRequest.getDestinationFloor() == constraints.getNumberOfFloors()) {
                    direction = Elevator.Direction.DOWN;
                } else {
                    boolean up = rand.nextInt(99) < 30;
                    direction = up ? Elevator.Direction.UP : Elevator.Direction.DOWN;
                }

                MoveRequest leaveRequest = new MoveRequest(moveRequest.getDestinationFloor(), 0, i + 1);
                int destFloor;
                switch (direction) {
                    case UP:
                        destFloor = rand.nextInt(constraints.getNumberOfFloors()) + 1;
                        if (destFloor == leaveRequest.getCurrentFloor()) {
                            destFloor++;
                        }

                        leaveRequest.setDestinationFloor(destFloor);
                        break;
                    case DOWN:
                        destFloor = rand.nextInt(moveRequest.getCurrentFloor()) + 1;
                        if (leaveRequest.getCurrentFloor() != 1 && destFloor == leaveRequest.getCurrentFloor()) {
                            destFloor--;
                        }

                        leaveRequest.setDestinationFloor(destFloor);
                        break;
                    default:
                        throw new UnsupportedOperationException("This should never happen.");
                }

                leaveRequest.validate();
                leaveQueue.add(leaveRequest);

                MoveRequest returnRequest = new MoveRequest(leaveRequest.getDestinationFloor(), leaveRequest.getCurrentFloor(), i + 2);
                returnRequest.validate();
                returnQueue.add(returnRequest);
            }
        }

        while (!leaveQueue.isEmpty()) {
            MoveRequest moveRequest = leaveQueue.poll();
            if (!pickupRequests.containsKey(moveRequest.getTargetTime())) {
                throw new UnsupportedOperationException("This should never happen.");
            }
            List<MoveRequest> moveRequestList = pickupRequests.get(moveRequest.getTargetTime());
            moveRequest.setTargetTime(null);
            int index = rand.nextInt(moveRequestList.size());
            moveRequestList.add(index, moveRequest);
        }
        return returnQueue;
    }

    /**
     * The intent of this method is to create more interesting scenario data.
     * Occupants that leave the building will return at a later time.
     */
    private void addReturnRequests(Map<Integer, List<MoveRequest>> pickupRequests, Queue<MoveRequest> returns) {
        Random rand = new Random(System.currentTimeMillis());
        while (!returns.isEmpty()) {
            MoveRequest returnRequest = returns.poll();
            assert returnRequest.getTargetTime() != null;
            int returnTime = rand.nextInt(constraints.getCheckinEndTime() - returnRequest.getTargetTime()) + returnRequest.getTargetTime();
            List<MoveRequest> moveRequestList = pickupRequests.computeIfAbsent(returnTime, _ -> new ArrayList<>());
            returnRequest.setTargetTime(null);
            moveRequestList.add(returnRequest);
        }
    }

    /**
     * Generate checkout pickup requests between checkout start time and checkout end time.
     */
    private void generateCheckoutPickupRequests(Map<Integer, List<MoveRequest>> pickupRequests) {
        Collection<Floor> floors = building.getFloors().values();
        Map<Room, Floor> roomFloorMap = new IdentityHashMap<>();
        for (Floor floor : floors) {
            if (floor.getRoomList().isEmpty()) {
                continue;
            }
            for (Room room : floor.getRoomList()) {
                if (room.getOccupants() == 0) {
                    continue;
                }
                roomFloorMap.put(room, floor);
            }
        }
        List<Room> occupiedRooms = building.getOccupiedRooms();

        List<Integer> remainingRooms = new ArrayList<>(occupiedRooms.size());
        for (int i = 0; i < occupiedRooms.size(); i++) {
            remainingRooms.add(i);
        }

        int checkoutsDuringPrimeTime = (int) Math.round(occupiedRooms.size() * .5);
        int checkoutsPerPrimeTimeHour = Math.round((float) checkoutsDuringPrimeTime / 2);

        int remainingCheckouts = occupiedRooms.size() - checkoutsDuringPrimeTime;
        int checkoutsPerNonPrimeTimeHour = Math.round((float) remainingCheckouts / (constraints.getCheckoutEndTime() - constraints.getCheckoutStartTime() - 2));

        Random rand = new Random(System.currentTimeMillis());
        for (int i = constraints.getCheckoutStartTime(); i <= constraints.getCheckoutEndTime(); i++) {
            timeSlices.add(i);
            boolean isBusyTime = i == 7 || i == 8;
            int checkouts = isBusyTime ? checkoutsPerPrimeTimeHour : checkoutsPerNonPrimeTimeHour;
            for (int j = 0; j < checkouts && !remainingRooms.isEmpty(); j++) {
                int index = rand.nextInt(remainingRooms.size());
                Room room = occupiedRooms.get(index);
                Floor floor = roomFloorMap.get(room);
                if (floor == null) {
                    throw new RuntimeException("No floor found for room " + room.getRoomNumber());
                }
                MoveRequest moveRequest = new MoveRequest(floor.getFloorNumber(), 1);
                moveRequest.validate();
                pickupRequests.computeIfAbsent(timeSlices.size(), _ -> new ArrayList<>()).add(moveRequest);
                remainingRooms.remove(index);
            }
        }
    }

    /**
     * The intent of this method is to create more interesting scenario data.
     * Each time slice should have the same number of pickup requests.
     */
    private void normalizePickupRequestsPerHour(Map<Integer, List<MoveRequest>> pickupRequests) {
        int maxRequestsPerHour = 0;
        for (List<MoveRequest> values : pickupRequests.values()) {
            if (maxRequestsPerHour < values.size()) {
                maxRequestsPerHour = values.size();
            }
        }

        // Fills each time slice with the max number of requests.
        // This allows each time slice to be split up evenly during the simulation.
        for (List<MoveRequest> values : pickupRequests.values()) {
            int difference = maxRequestsPerHour - values.size();
            if (difference > 0) {
                Random rand = new Random(System.currentTimeMillis());
                for (int j = 0; j < difference; j++) {
                    int index = rand.nextInt(values.size());
                    values.add(index, new MoveRequest(0, 0));
                }
            }
        }

        System.out.println();
    }

    private void printPickupRequests(Map<Integer, List<MoveRequest>> pickupRequests) {
        for (Map.Entry<Integer, List<MoveRequest>> entry : pickupRequests.entrySet()) {
            System.out.println("Time: " + entry.getKey());
            for (MoveRequest moveRequest : entry.getValue()) {
                System.out.println("  " + moveRequest);
            }
        }
    }
}
