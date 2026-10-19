package com.hostelmanagement.service;

import com.hostelmanagement.dao.RoomDAO;
import com.hostelmanagement.model.Room;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RoomService {

    private final RoomDAO roomDAO = new RoomDAO();

    public int addRoom(long floorId, long roomTypeId, String roomNumber)
            throws SQLException {

        validateRoomNumber(roomNumber);

        if (floorId <= 0 || roomTypeId <= 0) {
            throw new IllegalArgumentException(
                    "Select a valid floor and room type."
            );
        }

        return roomDAO.addRoomAndCreateBeds(
                floorId,
                roomTypeId,
                roomNumber.trim()
        );
    }

    public List<Room> getRoomsByFloor(long floorId)
            throws SQLException {

        if (floorId <= 0) {
            throw new IllegalArgumentException("Select a valid floor.");
        }

        return roomDAO.getRoomsByFloor(floorId);
    }

    public void updateRoom(long roomId, String roomNumber, String status)
            throws SQLException {

        if (roomId <= 0) {
            throw new IllegalArgumentException("Select a room first.");
        }

        validateRoomNumber(roomNumber);

        if (status == null
                || !Set.of("ACTIVE", "MAINTENANCE", "INACTIVE")
                .contains(status.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Select a valid room status.");
        }

        roomDAO.updateRoom(
                roomId,
                roomNumber.trim(),
                status.trim().toUpperCase(Locale.ROOT)
        );
    }

    private void validateRoomNumber(String roomNumber) {

        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }

        if (roomNumber.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "Room number must not exceed 30 characters."
            );
        }
    }
}