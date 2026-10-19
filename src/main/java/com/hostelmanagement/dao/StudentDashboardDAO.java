package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardDAO {

    public DashboardData getDashboardData(long userId)
            throws SQLException {

        String sql = """
                SELECT
                    u.user_id,
                    u.email,
                    u.account_status,
                    s.student_id,
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
                    s.registered_at,

                    h.hostel_name,
                    bl.block_name,
                    f.floor_name,
                    f.floor_number,
                    r.room_number,
                    b.bed_number,
                    rt.type_name AS room_type_name,
                    rt.capacity,
                    rt.default_monthly_fee,
                    ra.allocation_id

                FROM users u
                JOIN students s
                    ON s.user_id = u.user_id

                LEFT JOIN room_allocations ra
                    ON ra.student_id = s.student_id
                   AND ra.allocation_status = 'ACTIVE'

                LEFT JOIN beds b
                    ON b.bed_id = ra.bed_id

                LEFT JOIN rooms r
                    ON r.room_id = b.room_id

                LEFT JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id

                LEFT JOIN floors f
                    ON f.floor_id = r.floor_id

                LEFT JOIN blocks bl
                    ON bl.block_id = f.block_id

                LEFT JOIN hostels h
                    ON h.hostel_id = bl.hostel_id

                WHERE u.user_id = ?
                  AND u.role = 'STUDENT'

                ORDER BY ra.allocated_at DESC
                LIMIT 1
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Student profile was not found for this account."
                    );
                }

                long allocationId = rs.getLong("allocation_id");
                boolean hasAllocation = !rs.wasNull();

                String floorName = rs.getString("floor_name");
                int floorNumber = rs.getInt("floor_number");

                String floorDetails = floorName == null
                        ? null
                        : floorName + " (" + floorNumber + ")";

                java.math.BigDecimal fee =
                        rs.getBigDecimal("default_monthly_fee");

                return new DashboardData(
                        rs.getLong("user_id"),
                        rs.getLong("student_id"),
                        rs.getString("email"),
                        rs.getString("account_status"),
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
                        rs.getString("registered_at"),
                        rs.getString("hostel_name"),
                        rs.getString("block_name"),
                        floorDetails,
                        rs.getString("room_number"),
                        rs.getString("bed_number"),
                        rs.getString("room_type_name"),
                        rs.getInt("capacity"),
                        fee == null ? null : fee.toPlainString(),
                        hasAllocation
                );
            }
        }
    }

    public List<String> getRoommates(long userId)
            throws SQLException {

        String sql = """
                SELECT DISTINCT other.full_name

                FROM users currentUser

                JOIN students currentStudent
                    ON currentStudent.user_id = currentUser.user_id

                JOIN room_allocations currentAllocation
                    ON currentAllocation.student_id =
                       currentStudent.student_id
                   AND currentAllocation.allocation_status = 'ACTIVE'

                JOIN beds currentBed
                    ON currentBed.bed_id = currentAllocation.bed_id

                JOIN beds roommateBed
                    ON roommateBed.room_id = currentBed.room_id
                   AND roommateBed.bed_id <> currentBed.bed_id

                JOIN room_allocations roommateAllocation
                    ON roommateAllocation.bed_id = roommateBed.bed_id
                   AND roommateAllocation.allocation_status = 'ACTIVE'

                JOIN students other
                    ON other.student_id = roommateAllocation.student_id

                WHERE currentUser.user_id = ?
                  AND currentUser.role = 'STUDENT'
                  AND other.student_id <> currentStudent.student_id

                ORDER BY other.full_name
                """;

        List<String> roommates = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roommates.add(rs.getString("full_name"));
                }
            }
        }

        return roommates;
    }

    public static class DashboardData {

        private final long userId;
        private final long studentId;
        private final String email;
        private final String accountStatus;
        private final String fullName;
        private final String address;
        private final String phone;
        private final String profession;
        private final String institutionName;
        private final String organizationName;
        private final String dateOfBirth;
        private final String bloodGroup;
        private final String guardianName;
        private final String guardianPhone;
        private final String registeredAt;

        private final String hostelName;
        private final String blockName;
        private final String floorName;
        private final String roomNumber;
        private final String bedNumber;
        private final String roomTypeName;
        private final int capacity;
        private final String monthlyFee;
        private final boolean hasAllocation;

        public DashboardData(
                long userId,
                long studentId,
                String email,
                String accountStatus,
                String fullName,
                String address,
                String phone,
                String profession,
                String institutionName,
                String organizationName,
                String dateOfBirth,
                String bloodGroup,
                String guardianName,
                String guardianPhone,
                String registeredAt,
                String hostelName,
                String blockName,
                String floorName,
                String roomNumber,
                String bedNumber,
                String roomTypeName,
                int capacity,
                String monthlyFee,
                boolean hasAllocation
        ) {
            this.userId = userId;
            this.studentId = studentId;
            this.email = email;
            this.accountStatus = accountStatus;
            this.fullName = fullName;
            this.address = address;
            this.phone = phone;
            this.profession = profession;
            this.institutionName = institutionName;
            this.organizationName = organizationName;
            this.dateOfBirth = dateOfBirth;
            this.bloodGroup = bloodGroup;
            this.guardianName = guardianName;
            this.guardianPhone = guardianPhone;
            this.registeredAt = registeredAt;
            this.hostelName = hostelName;
            this.blockName = blockName;
            this.floorName = floorName;
            this.roomNumber = roomNumber;
            this.bedNumber = bedNumber;
            this.roomTypeName = roomTypeName;
            this.capacity = capacity;
            this.monthlyFee = monthlyFee;
            this.hasAllocation = hasAllocation;
        }

        public long getUserId() { return userId; }
        public long getStudentId() { return studentId; }
        public String getEmail() { return email; }
        public String getAccountStatus() { return accountStatus; }
        public String getFullName() { return fullName; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getProfession() { return profession; }
        public String getInstitutionName() { return institutionName; }
        public String getOrganizationName() { return organizationName; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getBloodGroup() { return bloodGroup; }
        public String getGuardianName() { return guardianName; }
        public String getGuardianPhone() { return guardianPhone; }
        public String getRegisteredAt() { return registeredAt; }
        public String getHostelName() { return hostelName; }
        public String getBlockName() { return blockName; }
        public String getFloorName() { return floorName; }
        public String getRoomNumber() { return roomNumber; }
        public String getBedNumber() { return bedNumber; }
        public String getRoomTypeName() { return roomTypeName; }
        public int getCapacity() { return capacity; }
        public String getMonthlyFee() { return monthlyFee; }
        public boolean hasAllocation() { return hasAllocation; }
    }
}