package com.hostelmanagement.service;

import com.hostelmanagement.dao.RoomTypeDAO;
import com.hostelmanagement.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

public class RoomTypeService {

    private final RoomTypeDAO roomTypeDAO = new RoomTypeDAO();

    public List<RoomType> getAllRoomTypes() throws SQLException {
        return roomTypeDAO.getAllRoomTypes();
    }

    public void addRoomType(String name, int capacity,
                            BigDecimal fee) throws SQLException {

        validate(name, capacity, fee);

        roomTypeDAO.addRoomType(
                name.trim(),
                capacity,
                fee.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public void updateRoomType(long id, String name, int capacity,
                               BigDecimal fee) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException("Select a room type first.");
        }

        validate(name, capacity, fee);

        roomTypeDAO.updateRoomType(
                id,
                name.trim(),
                capacity,
                fee.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public void deactivateRoomType(long id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException("Select a room type first.");
        }

        roomTypeDAO.deactivateRoomType(id);
    }

    private void validate(String name, int capacity, BigDecimal fee) {

        if (name == null || name.isBlank() || name.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Room type name is required (maximum 50 characters)."
            );
        }

        if (capacity < 1 || capacity > 100) {
            throw new IllegalArgumentException(
                    "Capacity must be between 1 and 100."
            );
        }

        if (fee == null || fee.signum() < 0
                || fee.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "Enter a valid non-negative monthly fee."
            );
        }
    }
}