package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.AvailableBed;
import com.hostelmanagement.model.PendingApplication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PendingApplicationDAO {

    public List<PendingApplication> getPendingApplications()
            throws SQLException {

        String sql = """
                SELECT
                    u.user_id,
                    s.student_id,
                    u.email,
                    s.full_name,
                    s.address,
                    s.phone,
                    s.profession,
                    s.institution_name,
                    s.organization_name,
                    s.date_of_birth,
                    s.blood_group,
                    s.guardian_name,
                    s.guardian_phone,
                    s.citizenship_id,
                    s.national_id,
                    s.preferred_room_type_id,
                    rt.type_name AS preferred_type_name,
                    rt.default_monthly_fee,
                    s.preferred_room_id,
                    pr.room_number AS preferred_room_number,
                    s.registered_at
                FROM users u
                JOIN students s ON s.user_id = u.user_id
                LEFT JOIN room_types rt
                    ON rt.room_type_id = s.preferred_room_type_id
                LEFT JOIN rooms pr
                    ON pr.room_id = s.preferred_room_id
                WHERE u.role = 'STUDENT'
                  AND u.account_status = 'PENDING'
                ORDER BY s.registered_at DESC, u.user_id DESC
                """;

        List<PendingApplication> applications = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                BigDecimalHolder fee = new BigDecimalHolder(
                        rs.getBigDecimal("default_monthly_fee")
                );

                applications.add(new PendingApplication(
                        rs.getLong("user_id"),
                        rs.getLong("student_id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("address"),
                        rs.getString("phone"),
                        rs.getString("profession"),
                        rs.getString("institution_name"),
                        rs.getString("organization_name"),
                        rs.getString("date_of_birth"),
                        rs.getString("blood_group"),
                        rs.getString("guardian_name"),
                        rs.getString("guardian_phone"),
                        rs.getString("citizenship_id"),
                        rs.getString("national_id"),
                        nullableLong(rs, "preferred_room_type_id"),
                        rs.getString("preferred_type_name"),
                        nullableLong(rs, "preferred_room_id"),
                        rs.getString("preferred_room_number"),
                        fee.value == null ? "" : fee.value.toPlainString(),
                        rs.getString("registered_at")
                ));
            }
        }

        return applications;
    }

    public List<AvailableBed> getAvailableBeds(long studentId)
            throws SQLException {

        String sql = """
                SELECT
                    b.bed_id,
                    b.room_id,
                    h.hostel_name,
                    bl.block_name,
                    f.floor_name,
                    r.room_number,
                    b.bed_number,
                    rt.type_name
                FROM students s
                JOIN beds b ON b.bed_status = 'AVAILABLE'
                JOIN rooms r ON r.room_id = b.room_id
                JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id
                   AND rt.is_active = TRUE
                JOIN floors f ON f.floor_id = r.floor_id
                JOIN blocks bl ON bl.block_id = f.block_id
                JOIN hostels h ON h.hostel_id = bl.hostel_id
                WHERE s.student_id = ?
                  AND r.room_status = 'ACTIVE'
                  AND (
                      s.preferred_room_type_id IS NULL
                      OR r.room_type_id = s.preferred_room_type_id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM room_allocations ra
                      WHERE ra.bed_id = b.bed_id
                        AND ra.allocation_status = 'ACTIVE'
                  )
                ORDER BY
                    CASE
                        WHEN r.room_id = s.preferred_room_id THEN 0
                        ELSE 1
                    END,
                    h.hostel_name,
                    bl.block_name,
                    f.floor_number,
                    r.room_number,
                    b.bed_number
                """;

        List<AvailableBed> beds = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    beds.add(new AvailableBed(
                            rs.getLong("bed_id"),
                            rs.getLong("room_id"),
                            rs.getString("hostel_name"),
                            rs.getString("block_name"),
                            rs.getString("floor_name"),
                            rs.getString("room_number"),
                            rs.getString("bed_number"),
                            rs.getString("type_name")
                    ));
                }
            }
        }

        return beds;
    }

    public void approveAndAllocate(
            long adminUserId,
            long userId,
            long studentId,
            long bedId
    ) throws SQLException {

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Verify that the acting user is an active Admin.
                String adminSql = """
                        SELECT user_id
                        FROM users
                        WHERE user_id = ?
                          AND role = 'ADMIN'
                          AND account_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps = con.prepareStatement(adminSql)) {
                    ps.setLong(1, adminUserId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "The logged-in Admin account is not active."
                            );
                        }
                    }
                }

                // Lock the pending application while processing it.
                String applicationSql = """
                        SELECT u.account_status,
                               s.preferred_room_type_id
                        FROM users u
                        JOIN students s ON s.user_id = u.user_id
                        WHERE u.user_id = ?
                          AND s.student_id = ?
                          AND u.role = 'STUDENT'
                        FOR UPDATE
                        """;

                Long preferredTypeId;

                try (PreparedStatement ps =
                             con.prepareStatement(applicationSql)) {

                    ps.setLong(1, userId);
                    ps.setLong(2, studentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Student application was not found."
                            );
                        }

                        if (!"PENDING".equals(rs.getString("account_status"))) {
                            throw new SQLException(
                                    "This application is no longer pending."
                            );
                        }

                        preferredTypeId =
                                nullableLong(rs, "preferred_room_type_id");
                    }
                }

                // A student cannot receive a second active allocation.
                String activeStudentSql = """
                        SELECT allocation_id
                        FROM room_allocations
                        WHERE student_id = ?
                          AND allocation_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(activeStudentSql)) {

                    ps.setLong(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new SQLException(
                                    "This student already has an active allocation."
                            );
                        }
                    }
                }

                // Lock and verify the selected bed and its room.
                String bedSql = """
                        SELECT b.room_id,
                               b.bed_status,
                               r.room_status,
                               r.room_type_id,
                               rt.is_active
                        FROM beds b
                        JOIN rooms r ON r.room_id = b.room_id
                        JOIN room_types rt
                            ON rt.room_type_id = r.room_type_id
                        WHERE b.bed_id = ?
                        FOR UPDATE
                        """;

                long actualRoomTypeId;

                try (PreparedStatement ps = con.prepareStatement(bedSql)) {
                    ps.setLong(1, bedId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Selected bed was not found.");
                        }

                        if (!"AVAILABLE".equals(rs.getString("bed_status"))
                                || !"ACTIVE".equals(rs.getString("room_status"))
                                || !rs.getBoolean("is_active")) {
                            throw new SQLException(
                                    "Selected bed, room or room type is not active."
                            );
                        }

                        actualRoomTypeId = rs.getLong("room_type_id");
                    }
                }

                if (preferredTypeId != null
                        && preferredTypeId.longValue() != actualRoomTypeId) {
                    throw new SQLException(
                            "Selected bed does not match the preferred room type."
                    );
                }

                // Recheck that this bed has not already been allocated.
                String activeBedSql = """
                        SELECT allocation_id
                        FROM room_allocations
                        WHERE bed_id = ?
                          AND allocation_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(activeBedSql)) {

                    ps.setLong(1, bedId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new SQLException(
                                    "This bed has already been allocated."
                            );
                        }
                    }
                }

                // FIX: allocated_by is required by your database schema.
                String insertAllocationSql = """
                        INSERT INTO room_allocations
                            (student_id, bed_id, allocated_at,
                             allocation_status, allocated_by)
                        VALUES (?, ?, CURRENT_TIMESTAMP, 'ACTIVE', ?)
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(insertAllocationSql)) {

                    ps.setLong(1, studentId);
                    ps.setLong(2, bedId);
                    ps.setLong(3, adminUserId);
                    ps.executeUpdate();
                }

                // Activate only after the allocation has been inserted.
                String activateSql = """
                        UPDATE users
                        SET account_status = 'ACTIVE'
                        WHERE user_id = ?
                          AND role = 'STUDENT'
                          AND account_status = 'PENDING'
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(activateSql)) {

                    ps.setLong(1, userId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Account activation failed."
                        );
                    }
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

    public void rejectApplication(long adminUserId, long userId, long studentId)
            throws SQLException {

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String adminSql = """
                        SELECT user_id
                        FROM users
                        WHERE user_id = ?
                          AND role = 'ADMIN'
                          AND account_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps = con.prepareStatement(adminSql)) {
                    ps.setLong(1, adminUserId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "The logged-in Admin account is not active."
                            );
                        }
                    }
                }

                String updateSql = """
                        UPDATE users u
                        JOIN students s ON s.user_id = u.user_id
                        SET u.account_status = 'REJECTED'
                        WHERE u.user_id = ?
                          AND s.student_id = ?
                          AND u.role = 'STUDENT'
                          AND u.account_status = 'PENDING'
                        """;

                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, studentId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Application was not found or is no longer pending."
                        );
                    }
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

    private static Long nullableLong(ResultSet rs, String column)
            throws SQLException {

        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static final class BigDecimalHolder {
        private final java.math.BigDecimal value;

        private BigDecimalHolder(java.math.BigDecimal value) {
            this.value = value;
        }
    }
}