package com.hostelmanagement.model;

public class Hostel {

    private final long hostelId;
    private final String hostelName;
    private final String hostelCode;
    private final String address;
    private final String hostelType;
    private final boolean active;

    public Hostel(long hostelId, String hostelName, String hostelCode,
                  String address, String hostelType, boolean active) {
        this.hostelId = hostelId;
        this.hostelName = hostelName;
        this.hostelCode = hostelCode;
        this.address = address;
        this.hostelType = hostelType;
        this.active = active;
    }

    public long getHostelId() {
        return hostelId;
    }

    public String getHostelName() {
        return hostelName;
    }

    public String getHostelCode() {
        return hostelCode;
    }

    public String getAddress() {
        return address;
    }

    public String getHostelType() {
        return hostelType;
    }

    public boolean isActive() {
        return active;
    }
    @Override
public String toString() {
    return hostelName + " (" + hostelCode + ")";
}
}
