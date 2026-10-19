package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.RoomType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomTypeDAO {

    public List<RoomType> getAllRoomTypes() throws SQLException {

        String sql = """
                SELECT room_type_id, type_name, capacity,
                       default_monthly_fee, is_active
                FROM room_types
                ORDER BY type_name
                """;

        List<RoomType> types = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                types.add(new RoomType(
                        rs.getLong("room_type_id"),
                        rs.getString("type_name"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("default_monthly_fee"),
                        rs.getBoolean("is_active")
                ));
            }
        }

        return types;
    }

    public void addRoomType(String name, int capacity,
                            BigDecimal monthlyFee) throws SQLException {

        String sql = """
                INSERT INTO room_types
                    (type_name, capacity, default_monthly_fee, is_active)
                VALUES (?, ?, ?, TRUE)
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, capacity);
            ps.setBigDecimal(3, monthlyFee);
            ps.executeUpdate();
        }
    }

    public void updateRoomType(long id, String name, int capacity,
                               BigDecimal monthlyFee) throws SQLException {

        String sql = """
                UPDATE room_types
                SET type_name = ?, capacity = ?, default_monthly_fee = ?
                WHERE room_type_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, capacity);
            ps.setBigDecimal(3, monthlyFee);
            ps.setLong(4, id);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Room type not found.");
            }
        }
    }

    public void deactivateRoomType(long id) throws SQLException {

        String sql = """
                UPDATE room_types
                SET is_active = FALSE
                WHERE room_type_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Room type not found.");
            }
        }
    }
}