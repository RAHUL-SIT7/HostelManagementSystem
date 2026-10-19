package com.hostelmanagement.model;

public class Room {

    private final long roomId;
    private final long floorId;
    private final long roomTypeId;
    private final String roomNumber;
    private final String roomStatus;
    private final String roomTypeName;
    private final int capacity;
    private final int totalBeds;
    private final int availableBeds;

    public Room(long roomId, long floorId, long roomTypeId,
                String roomNumber, String roomStatus,
                String roomTypeName, int capacity,
                int totalBeds, int availableBeds) {
        this.roomId = roomId;
        this.floorId = floorId;
        this.roomTypeId = roomTypeId;
        this.roomNumber = roomNumber;
        this.roomStatus = roomStatus;
        this.roomTypeName = roomTypeName;
        this.capacity = capacity;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
    }

    public long getRoomId() {
        return roomId;
    }

    public long getFloorId() {
        return floorId;
    }

    public long getRoomTypeId() {
        return roomTypeId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }
}