package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.Block;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockDAO {

    public void addBlock(long hostelId, String name, String code)
            throws SQLException {

        String sql = """
                INSERT INTO blocks (hostel_id, block_name, block_code)
                VALUES (?, ?, ?)
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, hostelId);
            ps.setString(2, name);
            ps.setString(3, code);
            ps.executeUpdate();
        }
    }

    public List<Block> getBlocksByHostel(long hostelId)
            throws SQLException {

        String sql = """
                SELECT block_id, hostel_id, block_name, block_code
                FROM blocks
                WHERE hostel_id = ?
                ORDER BY block_id
                """;

        List<Block> blocks = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, hostelId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(new Block(
                            rs.getLong("block_id"),
                            rs.getLong("hostel_id"),
                            rs.getString("block_name"),
                            rs.getString("block_code")
                    ));
                }
            }
        }

        return blocks;
    }

    public void updateBlock(long blockId, String name, String code)
            throws SQLException {

        String sql = """
                UPDATE blocks
                SET block_name = ?, block_code = ?
                WHERE block_id = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, code);
            ps.setLong(3, blockId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Block not found.");
            }
        }
    }
}