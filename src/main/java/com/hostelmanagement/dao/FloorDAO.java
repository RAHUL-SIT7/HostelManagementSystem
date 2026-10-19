package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.Floor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FloorDAO {

    public void addFloor(long blockId, int floorNumber,
                         String floorName) throws SQLException {

        String sql = """
                INSERT INTO floors (block_id, floor_number, floor_name)
                VALUES (?, ?, ?)
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, blockId);
            ps.setInt(2, floorNumber);
            ps.setString(3, floorName);

            ps.executeUpdate();
        }
    }

    public List<Floor> getFloorsByBlock(long blockId)
            throws SQLException {

        String sql = """
                SELECT floor_id, block_id, floor_number, floor_name
                FROM floors
                WHERE block_id = ?
                ORDER BY floor_number
                """;

        List<Floor> floors = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, blockId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    floors.add(new Floor(
                            rs.getLong("floor_id"),
                            rs.getLong("block_id"),
                            rs.getInt("floor_number"),
                            rs.getString("floor_name")
                    ));
                }
            }
        }

        return floors;
    }

    public void updateFloor(long floorId, int floorNumber,
                            String floorName) throws SQLException {

        String sql = """
                UPDATE floors
                SET floor_number = ?, floor_name = ?
                WHERE floor_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, floorNumber);
            ps.setString(2, floorName);
            ps.setLong(3, floorId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Floor not found.");
            }
        }
    }
}