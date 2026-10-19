package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.Hostel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HostelDAO {

    public void addHostel(String name, String code,
                          String address, String type) throws SQLException {

        String sql = """
                INSERT INTO hostels
                    (hostel_name, hostel_code, address, hostel_type, is_active)
                VALUES (?, ?, ?, ?, TRUE)
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, code);
            ps.setString(3, address);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    public List<Hostel> getAllHostels() throws SQLException {

        String sql = """
                SELECT hostel_id, hostel_name, hostel_code,
                       address, hostel_type, is_active
                FROM hostels
                ORDER BY hostel_id DESC
                """;

        List<Hostel> hostels = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                hostels.add(new Hostel(
                        rs.getLong("hostel_id"),
                        rs.getString("hostel_name"),
                        rs.getString("hostel_code"),
                        rs.getString("address"),
                        rs.getString("hostel_type"),
                        rs.getBoolean("is_active")
                ));
            }
        }

        return hostels;
    }

    public void updateHostel(long id, String name, String code,
                             String address, String type)
            throws SQLException {

        String sql = """
                UPDATE hostels
                SET hostel_name = ?, hostel_code = ?,
                    address = ?, hostel_type = ?
                WHERE hostel_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, code);
            ps.setString(3, address);
            ps.setString(4, type);
            ps.setLong(5, id);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Hostel not found.");
            }
        }
    }

    public void deactivateHostel(long id) throws SQLException {

        String sql = """
                UPDATE hostels
                SET is_active = FALSE
                WHERE hostel_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Hostel not found.");
            }
        }
    }
}