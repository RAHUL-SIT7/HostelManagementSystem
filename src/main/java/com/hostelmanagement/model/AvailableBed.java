package com.hostelmanagement.model;

public class AvailableBed {

    private final long bedId;
    private final long roomId;
    private final String hostelName;
    private final String blockName;
    private final String floorName;
    private final String roomNumber;
    private final String bedNumber;
    private final String roomTypeName;

    public AvailableBed(
            long bedId,
            long roomId,
            String hostelName,
            String blockName,
            String floorName,
            String roomNumber,
            String bedNumber,
            String roomTypeName
    ) {
        this.bedId = bedId;
        this.roomId = roomId;
        this.hostelName = hostelName;
        this.blockName = blockName;
        this.floorName = floorName;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.roomTypeName = roomTypeName;
    }

    public long getBedId() { return bedId; }
    public long getRoomId() { return roomId; }
    public String getHostelName() { return hostelName; }
    public String getBlockName() { return blockName; }
    public String getFloorName() { return floorName; }
    public String getRoomNumber() { return roomNumber; }
    public String getBedNumber() { return bedNumber; }
    public String getRoomTypeName() { return roomTypeName; }

    @Override
    public String toString() {
        return hostelName + " / " + blockName + " / " + floorName
                + " / Room " + roomNumber + " / Bed " + bedNumber
                + " (" + roomTypeName + ")";
    }
}