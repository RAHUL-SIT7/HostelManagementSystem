package com.hostelmanagement.model;

public class Floor {

    private final long floorId;
    private final long blockId;
    private final int floorNumber;
    private final String floorName;

    public Floor(long floorId, long blockId,
                 int floorNumber, String floorName) {
        this.floorId = floorId;
        this.blockId = blockId;
        this.floorNumber = floorNumber;
        this.floorName = floorName;
    }

    public long getFloorId() {
        return floorId;
    }

    public long getBlockId() {
        return blockId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    @Override
    public String toString() {
        return floorName + " (" + floorNumber + ")";
    }
}