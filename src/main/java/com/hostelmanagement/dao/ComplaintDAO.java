package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Database access for maintenance complaints. Enum options are read from the actual DB schema. */
public class ComplaintDAO {

    public static class RoomInfo {
        public final long roomId;
        public final String hostel;
        public final String block;
        public final String floor;
        public final String roomNumber;
        public final String bedNumber;

        public RoomInfo(long roomId, String hostel, String block, String floor,
                        String roomNumber, String bedNumber) {
            this.roomId = roomId;
            this.hostel = hostel;
            this.block = block;
            this.floor = floor;
            this.roomNumber = roomNumber;
            this.bedNumber = bedNumber;
        }

        @Override public String toString() {
            return hostel + " / Block " + block + " / " + floor
                    + " / Room " + roomNumber + " / Bed " + bedNumber;
        }
    }

    public static class ComplaintRecord {
        public final long complaintId;
        public final long studentId;
        public final String studentName;
        public final String email;
        public final String roomNumber;
        public final String category;
        public final String otherProfessional;
        public final String description;
        public final String status;
        public final String resolutionNote;
        public final LocalDateTime createdAt;

        public ComplaintRecord(long complaintId, long studentId, String studentName, String email,
                               String roomNumber, String category, String otherProfessional,
                               String description, String status, String resolutionNote,
                               LocalDateTime createdAt) {
            this.complaintId = complaintId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.email = email;
            this.roomNumber = roomNumber;
            this.category = category;
            this.otherProfessional = otherProfessional;
            this.description = description;
            this.status = status;
            this.resolutionNote = resolutionNote;
            this.createdAt = createdAt;
        }
    }

    private void requireActiveRole(Connection con, long userId, String role) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE user_id = ? AND role = ? AND account_status = 'ACTIVE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("An active " + role + " account is required.");
            }
        }
    }

    private long activeStudentId(Connection con, long userId) throws SQLException {
        String sql = """
                SELECT s.student_id FROM users u JOIN students s ON s.user_id = u.user_id
                WHERE u.user_id = ? AND u.role = 'STUDENT' AND u.account_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("An active student profile is required.");
                return rs.getLong("student_id");
            }
        }
    }

    private List<String> enumOptions(Connection con, String column) throws SQLException {
        String sql = """
                SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaints' AND COLUMN_NAME = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Could not find complaints." + column + " in the current database.");
                String type = rs.getString("COLUMN_TYPE");
                if (type == null || !type.toLowerCase().startsWith("enum(")) {
                    throw new SQLException("complaints." + column + " is not an ENUM column as expected.");
                }
                String inner = type.substring(type.indexOf('(') + 1, type.lastIndexOf(')'));
                List<String> values = new ArrayList<>();
                // The existing category/status values are plain ENUM labels with no commas.
                for (String part : inner.split(",(?=(?:[^']*'[^']*')*[^']*$)")) {
                    String value = part.trim();
                    if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1).replace("\\'", "'").replace("''", "'");
                    }
                    if (!value.isBlank()) values.add(value);
                }
                return values;
            }
        }
    }

    private RoomInfo findCurrentRoom(Connection con, long userId) throws SQLException {
        String sql = """
                SELECT r.room_id, h.hostel_name, bl.block_name,
                       CONCAT(f.floor_name, ' (', f.floor_number, ')') AS floor_label,
                       r.room_number, b.bed_number
                FROM users u
                JOIN students s ON s.user_id = u.user_id
                JOIN room_allocations ra ON ra.student_id = s.student_id
                                             AND ra.allocation_status = 'ACTIVE'
                JOIN beds b ON b.bed_id = ra.bed_id
                JOIN rooms r ON r.room_id = b.room_id
                JOIN floors f ON f.floor_id = r.floor_id
                JOIN blocks bl ON bl.block_id = f.block_id
                JOIN hostels h ON h.hostel_id = bl.hostel_id
                WHERE u.user_id = ? AND u.role = 'STUDENT'
                  AND u.account_status = 'ACTIVE'
                LIMIT 1
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("You need an active room/bed allocation to submit a maintenance complaint.");
                return new RoomInfo(rs.getLong("room_id"), rs.getString("hostel_name"),
                        rs.getString("block_name"), rs.getString("floor_label"),
                        rs.getString("room_number"), rs.getString("bed_number"));
            }
        }
    }

    public RoomInfo getCurrentRoom(long studentUserId) throws SQLException {
        try (Connection con = MySQLConnection.getConnection()) {
            return findCurrentRoom(con, studentUserId);
        }
    }

    public List<String> getCategories(long studentUserId) throws SQLException {
        try (Connection con = MySQLConnection.getConnection()) {
            activeStudentId(con, studentUserId);
            return enumOptions(con, "category");
        }
    }

    public List<String> getStatuses(long adminUserId) throws SQLException {
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            return enumOptions(con, "complaint_status");
        }
    }

    public long submitComplaint(long studentUserId, String category,
                                String otherProfessional, String description) throws SQLException {
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Choose a problem category.");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Describe the problem.");
        if (description.trim().length() > 5000) throw new IllegalArgumentException("Description must be 5000 characters or less.");
        if (otherProfessional != null && otherProfessional.length() > 100) throw new IllegalArgumentException("Professional name must be 100 characters or less.");

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                long studentId = activeStudentId(con, studentUserId);
                List<String> categories = enumOptions(con, "category");
                if (!categories.contains(category)) throw new IllegalArgumentException("Choose a supported complaint category.");
                RoomInfo room = findCurrentRoom(con, studentUserId);

                String sql = """
                        INSERT INTO complaints
                          (student_id, room_id, category, other_professional, description,
                           complaint_status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """;
                long id;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, studentId);
                    ps.setLong(2, room.roomId);
                    ps.setString(3, category);
                    if (otherProfessional == null || otherProfessional.isBlank()) ps.setNull(4, Types.VARCHAR);
                    else ps.setString(4, otherProfessional.trim());
                    ps.setString(5, description.trim());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not obtain complaint ID.");
                        id = keys.getLong(1);
                    }
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<ComplaintRecord> getStudentComplaints(long studentUserId) throws SQLException {
        String sql = """
                SELECT c.complaint_id, s.student_id, s.full_name, u.email,
                       r.room_number, c.category, c.other_professional,
                       c.description, c.complaint_status, c.resolution_note, c.created_at
                FROM users u
                JOIN students s ON s.user_id = u.user_id
                JOIN complaints c ON c.student_id = s.student_id
                LEFT JOIN rooms r ON r.room_id = c.room_id
                WHERE u.user_id = ? AND u.role = 'STUDENT' AND u.account_status = 'ACTIVE'
                ORDER BY c.created_at DESC, c.complaint_id DESC
                """;
        List<ComplaintRecord> rows = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            activeStudentId(con, studentUserId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, studentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) rows.add(readRecord(rs));
                }
            }
        }
        return rows;
    }

    public List<ComplaintRecord> getAllComplaints(long adminUserId) throws SQLException {
        String sql = """
                SELECT c.complaint_id, s.student_id, s.full_name, u.email,
                       r.room_number, c.category, c.other_professional,
                       c.description, c.complaint_status, c.resolution_note, c.created_at
                FROM complaints c
                JOIN students s ON s.student_id = c.student_id
                JOIN users u ON u.user_id = s.user_id
                LEFT JOIN rooms r ON r.room_id = c.room_id
                ORDER BY CASE WHEN c.complaint_status = 'PENDING' THEN 0
                              WHEN c.complaint_status = 'IN_PROGRESS' THEN 1 ELSE 2 END,
                         c.created_at DESC, c.complaint_id DESC
                """;
        List<ComplaintRecord> rows = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(readRecord(rs));
            }
        }
        return rows;
    }

    private ComplaintRecord readRecord(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new ComplaintRecord(rs.getLong("complaint_id"), rs.getLong("student_id"),
                rs.getString("full_name"), rs.getString("email"), rs.getString("room_number"),
                rs.getString("category"), rs.getString("other_professional"), rs.getString("description"),
                rs.getString("complaint_status"), rs.getString("resolution_note"),
                ts == null ? null : ts.toLocalDateTime());
    }

    public void updateComplaint(long adminUserId, long complaintId,
                                String status, String resolutionNote) throws SQLException {
        if (complaintId <= 0) throw new IllegalArgumentException("Select a valid complaint.");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("Choose a complaint status.");
        if (resolutionNote != null && resolutionNote.length() > 1000) throw new IllegalArgumentException("Resolution note must be 1000 characters or less.");

        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            if (!enumOptions(con, "complaint_status").contains(status)) {
                throw new IllegalArgumentException("Choose a status supported by the current database.");
            }
            boolean resolved = status.equalsIgnoreCase("SOLVED")
                    || status.equalsIgnoreCase("RESOLVED")
                    || status.equalsIgnoreCase("CLOSED");
            String sql = """
                    UPDATE complaints
                    SET complaint_status = ?, resolution_note = ?, resolved_by = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE complaint_id = ?
                    """;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, status);
                if (resolutionNote == null || resolutionNote.isBlank()) ps.setNull(2, Types.VARCHAR);
                else ps.setString(2, resolutionNote.trim());
                if (resolved) ps.setLong(3, adminUserId); else ps.setNull(3, Types.BIGINT);
                ps.setLong(4, complaintId);
                if (ps.executeUpdate() != 1) throw new SQLException("Complaint not found; refresh the list and retry.");
            }
        }
    }
}
