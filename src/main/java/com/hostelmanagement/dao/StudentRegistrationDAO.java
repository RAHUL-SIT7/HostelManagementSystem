package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.Room;
import com.hostelmanagement.model.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentRegistrationDAO {

    public List<RoomType> getActiveRoomTypes() throws SQLException {
        String sql = """
                SELECT room_type_id, type_name, capacity,
                       default_monthly_fee, is_active
                FROM room_types
                WHERE is_active = TRUE
                ORDER BY capacity
                """;

        List<RoomType> result = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new RoomType(
                        rs.getLong("room_type_id"),
                        rs.getString("type_name"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("default_monthly_fee"),
                        rs.getBoolean("is_active")
                ));
            }
        }

        return result;
    }

    public List<Room> getAvailableRoomsByType(long roomTypeId)
            throws SQLException {

        String sql = """
                SELECT r.room_id, r.floor_id, r.room_type_id,
                       r.room_number, r.room_status,
                       rt.type_name, rt.capacity,
                       COUNT(b.bed_id) AS total_beds,
                       COALESCE(SUM(
                           CASE WHEN b.bed_status = 'AVAILABLE'
                             AND NOT EXISTS (
                                 SELECT 1
                                 FROM room_allocations ra
                                 WHERE ra.bed_id = b.bed_id
                                   AND ra.allocation_status = 'ACTIVE'
                             )
                           THEN 1 ELSE 0 END
                       ), 0) AS available_beds
                FROM rooms r
                JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id
                LEFT JOIN beds b
                    ON b.room_id = r.room_id
                WHERE r.room_type_id = ?
                  AND r.room_status = 'ACTIVE'
                  AND rt.is_active = TRUE
                GROUP BY r.room_id, r.floor_id, r.room_type_id,
                         r.room_number, r.room_status,
                         rt.type_name, rt.capacity
                HAVING available_beds > 0
                ORDER BY r.room_number
                """;

        List<Room> result = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, roomTypeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Room(
                            rs.getLong("room_id"),
                            rs.getLong("floor_id"),
                            rs.getLong("room_type_id"),
                            rs.getString("room_number"),
                            rs.getString("room_status"),
                            rs.getString("type_name"),
                            rs.getInt("capacity"),
                            rs.getInt("total_beds"),
                            rs.getInt("available_beds")
                    ));
                }
            }
        }

        return result;
    }

    public void submitApplication(
            String email,
            String passwordHash,
            String fullName,
            String address,
            String phone,
            String profession,
            String institutionName,
            String organizationName,
            Date dateOfBirth,
            String bloodGroup,
            String guardianName,
            String guardianPhone,
            String citizenshipId,
            String nationalId,
            long preferredRoomTypeId,
            long preferredRoomId
    ) throws SQLException {

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Recheck the preferred room before saving the application.
                String roomCheckSql = """
                        SELECT COUNT(*)
                        FROM rooms r
                        JOIN room_types rt
                            ON rt.room_type_id = r.room_type_id
                        WHERE r.room_id = ?
                          AND r.room_type_id = ?
                          AND r.room_status = 'ACTIVE'
                          AND rt.is_active = TRUE
                          AND EXISTS (
                              SELECT 1
                              FROM beds b
                              WHERE b.room_id = r.room_id
                                AND b.bed_status = 'AVAILABLE'
                                AND NOT EXISTS (
                                    SELECT 1
                                    FROM room_allocations ra
                                    WHERE ra.bed_id = b.bed_id
                                      AND ra.allocation_status = 'ACTIVE'
                                )
                          )
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(roomCheckSql)) {
                    ps.setLong(1, preferredRoomId);
                    ps.setLong(2, preferredRoomTypeId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();

                        if (rs.getInt(1) == 0) {
                            throw new SQLException(
                                    "The selected room is no longer available. "
                                            + "Please select another room."
                            );
                        }
                    }
                }

                String userSql = """
                        INSERT INTO users
                            (email, password_hash, role, account_status)
                        VALUES (?, ?, 'STUDENT', 'PENDING')
                        """;

                long userId;

                try (PreparedStatement ps = con.prepareStatement(
                        userSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, email);
                    ps.setString(2, passwordHash);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not create the user account."
                            );
                        }

                        userId = keys.getLong(1);
                    }
                }

                String studentSql = """
                        INSERT INTO students (
                            user_id, full_name, address, phone, profession,
                            institution_name, organization_name,
                            date_of_birth, blood_group,
                            guardian_name, guardian_phone,
                            citizenship_id, national_id,
                            preferred_room_type_id, preferred_room_id
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(studentSql)) {

                    ps.setLong(1, userId);
                    ps.setString(2, fullName);
                    ps.setString(3, address);
                    ps.setString(4, phone);
                    ps.setString(5, profession);
                    ps.setString(6, institutionName);
                    ps.setString(7, organizationName);
                    ps.setDate(8, dateOfBirth);
                    ps.setString(9, bloodGroup);
                    ps.setString(10, guardianName);
                    ps.setString(11, guardianPhone);
                    ps.setString(12, citizenshipId);
                    ps.setString(13, nationalId);
                    ps.setLong(14, preferredRoomTypeId);
                    ps.setLong(15, preferredRoomId);

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;

            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}