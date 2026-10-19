package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HostelAvailabilityDAO {

    // One summary row for every hostel, including hostels
    // that do not have any rooms yet.
    public List<Object[]> getHostelSummary() throws SQLException {

        String sql = """
                SELECT
                    h.hostel_name,
                    COUNT(DISTINCT r.room_id) AS total_rooms,
                    COUNT(DISTINCT b.bed_id) AS total_beds,

                    COUNT(DISTINCT CASE
                        WHEN ra.allocation_id IS NOT NULL
                        THEN b.bed_id
                    END) AS occupied_beds,

                    COUNT(DISTINCT CASE
                        WHEN r.room_status = 'ACTIVE'
                         AND rt.is_active = TRUE
                         AND b.bed_status = 'AVAILABLE'
                         AND ra.allocation_id IS NULL
                        THEN b.bed_id
                    END) AS vacant_beds,

                    COUNT(DISTINCT CASE
                        WHEN b.bed_status = 'MAINTENANCE'
                        THEN b.bed_id
                    END) AS maintenance_beds

                FROM hostels h
                LEFT JOIN blocks bl
                    ON bl.hostel_id = h.hostel_id
                LEFT JOIN floors f
                    ON f.block_id = bl.block_id
                LEFT JOIN rooms r
                    ON r.floor_id = f.floor_id
                LEFT JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id
                LEFT JOIN beds b
                    ON b.room_id = r.room_id
                LEFT JOIN room_allocations ra
                    ON ra.bed_id = b.bed_id
                   AND ra.allocation_status = 'ACTIVE'

                GROUP BY h.hostel_id, h.hostel_name
                ORDER BY h.hostel_name
                """;

        List<Object[]> rows = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString("hostel_name"),
                        rs.getInt("total_rooms"),
                        rs.getInt("total_beds"),
                        rs.getInt("occupied_beds"),
                        rs.getInt("vacant_beds"),
                        rs.getInt("maintenance_beds")
                });
            }
        }

        return rows;
    }

    // One row for every room, showing its current bed availability.
    public List<Object[]> getRoomAvailability() throws SQLException {

        String sql = """
                SELECT
                    h.hostel_name,
                    bl.block_name,
                    f.floor_name,
                    f.floor_number,
                    r.room_number,
                    r.room_status,
                    rt.type_name,
                    rt.capacity,

                    COUNT(DISTINCT b.bed_id) AS total_beds,

                    COUNT(DISTINCT CASE
                        WHEN ra.allocation_id IS NOT NULL
                        THEN b.bed_id
                    END) AS occupied_beds,

                    COUNT(DISTINCT CASE
                        WHEN r.room_status = 'ACTIVE'
                         AND rt.is_active = TRUE
                         AND b.bed_status = 'AVAILABLE'
                         AND ra.allocation_id IS NULL
                        THEN b.bed_id
                    END) AS vacant_beds,

                    COUNT(DISTINCT CASE
                        WHEN b.bed_status = 'MAINTENANCE'
                        THEN b.bed_id
                    END) AS maintenance_beds

                FROM hostels h
                JOIN blocks bl
                    ON bl.hostel_id = h.hostel_id
                JOIN floors f
                    ON f.block_id = bl.block_id
                JOIN rooms r
                    ON r.floor_id = f.floor_id
                JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id

                LEFT JOIN beds b
                    ON b.room_id = r.room_id
                LEFT JOIN room_allocations ra
                    ON ra.bed_id = b.bed_id
                   AND ra.allocation_status = 'ACTIVE'

                GROUP BY
                    h.hostel_name,
                    bl.block_name,
                    f.floor_name,
                    f.floor_number,
                    r.room_id,
                    r.room_number,
                    r.room_status,
                    rt.type_name,
                    rt.capacity,
                    rt.is_active

                ORDER BY
                    h.hostel_name,
                    bl.block_name,
                    f.floor_number,
                    r.room_number
                """;

        List<Object[]> rows = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String roomStatus = rs.getString("room_status");
                int totalBeds = rs.getInt("total_beds");
                int occupiedBeds = rs.getInt("occupied_beds");
                int vacantBeds = rs.getInt("vacant_beds");
                int maintenanceBeds = rs.getInt("maintenance_beds");

                String availability = getAvailabilityStatus(
                        roomStatus,
                        totalBeds,
                        occupiedBeds,
                        vacantBeds,
                        maintenanceBeds
                );

                rows.add(new Object[]{
                        rs.getString("hostel_name"),
                        rs.getString("block_name"),
                        rs.getString("floor_name")
                                + " (" + rs.getInt("floor_number") + ")",
                        rs.getString("room_number"),
                        rs.getString("type_name"),
                        rs.getInt("capacity"),
                        totalBeds,
                        occupiedBeds,
                        vacantBeds,
                        maintenanceBeds,
                        availability
                });
            }
        }

        return rows;
    }

    private String getAvailabilityStatus(
            String roomStatus,
            int totalBeds,
            int occupiedBeds,
            int vacantBeds,
            int maintenanceBeds
    ) {
        if (roomStatus != null
                && !"ACTIVE".equalsIgnoreCase(roomStatus)) {
            return roomStatus;
        }

        if (totalBeds == 0) {
            return "NO BEDS";
        }

        if (vacantBeds == totalBeds) {
            return "VACANT";
        }

        if (occupiedBeds >= totalBeds) {
            return "FULL";
        }

        if (vacantBeds > 0 && occupiedBeds > 0) {
            return "PARTIALLY OCCUPIED";
        }

        if (maintenanceBeds > 0) {
            return "MAINTENANCE / UNAVAILABLE";
        }

        if (vacantBeds > 0) {
            return "AVAILABLE";
        }

        return "NO VACANT BEDS";
    }
}