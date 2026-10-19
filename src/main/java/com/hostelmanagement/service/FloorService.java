package com.hostelmanagement.service;

import com.hostelmanagement.dao.FloorDAO;
import com.hostelmanagement.model.Floor;

import java.sql.SQLException;
import java.util.List;

public class FloorService {

    private final FloorDAO floorDAO = new FloorDAO();

    public List<Floor> getFloorsByBlock(long blockId)
            throws SQLException {

        if (blockId <= 0) {
            throw new IllegalArgumentException("Select a valid block.");
        }

        return floorDAO.getFloorsByBlock(blockId);
    }

    public void addFloor(long blockId, int floorNumber,
                         String floorName) throws SQLException {

        validate(blockId, floorNumber, floorName);

        floorDAO.addFloor(
                blockId,
                floorNumber,
                floorName.trim()
        );
    }

    public void updateFloor(long floorId, int floorNumber,
                            String floorName) throws SQLException {

        if (floorId <= 0) {
            throw new IllegalArgumentException("Select a floor first.");
        }

        validate(1, floorNumber, floorName);

        floorDAO.updateFloor(
                floorId,
                floorNumber,
                floorName.trim()
        );
    }

    private void validate(long blockId, int floorNumber,
                          String floorName) {

        if (blockId <= 0) {
            throw new IllegalArgumentException("Select a block.");
        }

        if (floorNumber < -5 || floorNumber > 200) {
            throw new IllegalArgumentException(
                    "Floor number must be between -5 and 200."
            );
        }

        if (floorName == null || floorName.isBlank()
                || floorName.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Floor name is required and must not exceed 50 characters."
            );
        }
    }
}