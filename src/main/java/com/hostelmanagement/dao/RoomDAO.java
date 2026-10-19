package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public int addRoomAndCreateBeds(
            long floorId,
            long roomTypeId,
            String roomNumber
    ) throws SQLException {

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int capacity;

                String typeSql = """
                        SELECT capacity, is_active
                        FROM room_types
                        WHERE room_type_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement ps = con.prepareStatement(typeSql)) {
                    ps.setLong(1, roomTypeId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Room type not found.");
                        }

                        if (!rs.getBoolean("is_active")) {
                            throw new SQLException(
                                    "This room type is inactive."
                            );
                        }

                        capacity = rs.getInt("capacity");
                    }
                }

                String roomSql = """
                        INSERT INTO rooms
                            (floor_id, room_type_id, room_number, room_status)
                        VALUES (?, ?, ?, 'ACTIVE')
                        """;

                long roomId;

                try (PreparedStatement ps = con.prepareStatement(
                        roomSql,
                        PreparedStatement.RETURN_GENERATED_KEYS
                )) {
                    ps.setLong(1, floorId);
                    ps.setLong(2, roomTypeId);
                    ps.setString(3, roomNumber);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Room could not be created.");
                    }

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Room ID was not generated."
                            );
                        }

                        roomId = keys.getLong(1);
                    }
                }

                String bedSql = """
                        INSERT INTO beds (room_id, bed_number, bed_status)
                        VALUES (?, ?, 'AVAILABLE')
                        """;

                try (PreparedStatement ps = con.prepareStatement(bedSql)) {
                    for (int i = 1; i <= capacity; i++) {
                        ps.setLong(1, roomId);
                        ps.setString(2, String.format("B%02d", i));
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                con.commit();
                return capacity;

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

    public List<Room> getRoomsByFloor(long floorId)
            throws SQLException {

        String sql = """
                SELECT
                    r.room_id,
                    r.floor_id,
                    r.room_type_id,
                    r.room_number,
                    r.room_status,
                    rt.type_name,
                    rt.capacity,
                    COUNT(b.bed_id) AS total_beds,
                    COALESCE(
                        SUM(
                            CASE
                                WHEN b.bed_status = 'AVAILABLE' THEN 1
                                ELSE 0
                            END
                        ),
                        0
                    ) AS available_beds
                FROM rooms r
                JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id
                LEFT JOIN beds b
                    ON b.room_id = r.room_id
                WHERE r.floor_id = ?
                GROUP BY
                    r.room_id,
                    r.floor_id,
                    r.room_type_id,
                    r.room_number,
                    r.room_status,
                    rt.type_name,
                    rt.capacity
                ORDER BY r.room_number
                """;

        List<Room> rooms = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, floorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new Room(
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

        return rooms;
    }

    public void updateRoom(
            long roomId,
            String roomNumber,
            String roomStatus
    ) throws SQLException {

        String sql = """
                UPDATE rooms
                SET room_number = ?, room_status = ?
                WHERE room_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roomNumber);
            ps.setString(2, roomStatus);
            ps.setLong(3, roomId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Room not found.");
            }
        }
    }
}