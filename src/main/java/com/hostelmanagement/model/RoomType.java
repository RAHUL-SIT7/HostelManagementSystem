package com.hostelmanagement.model;

import java.math.BigDecimal;

public class RoomType {

    private final long roomTypeId;
    private final String typeName;
    private final int capacity;
    private final BigDecimal defaultMonthlyFee;
    private final boolean active;

    public RoomType(long roomTypeId, String typeName, int capacity,
                    BigDecimal defaultMonthlyFee, boolean active) {
        this.roomTypeId = roomTypeId;
        this.typeName = typeName;
        this.capacity = capacity;
        this.defaultMonthlyFee = defaultMonthlyFee;
        this.active = active;
    }

    public long getRoomTypeId() {
        return roomTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getDefaultMonthlyFee() {
        return defaultMonthlyFee;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return typeName + " (" + capacity + " beds)";
    }
}