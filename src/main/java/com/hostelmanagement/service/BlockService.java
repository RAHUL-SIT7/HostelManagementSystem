package com.hostelmanagement.service;

import com.hostelmanagement.dao.BlockDAO;
import com.hostelmanagement.model.Block;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class BlockService {

    private final BlockDAO blockDAO = new BlockDAO();

    public List<Block> getBlocksByHostel(long hostelId)
            throws SQLException {

        if (hostelId <= 0) {
            throw new IllegalArgumentException("Select a valid hostel.");
        }

        return blockDAO.getBlocksByHostel(hostelId);
    }

    public void addBlock(long hostelId, String name, String code)
            throws SQLException {

        validate(hostelId, name, code);

        blockDAO.addBlock(
                hostelId,
                name.trim(),
                code.trim().toUpperCase(Locale.ROOT)
        );
    }

    public void updateBlock(long blockId, String name, String code)
            throws SQLException {

        if (blockId <= 0) {
            throw new IllegalArgumentException("Select a block first.");
        }

        validate(1, name, code);

        blockDAO.updateBlock(
                blockId,
                name.trim(),
                code.trim().toUpperCase(Locale.ROOT)
        );
    }

    private void validate(long hostelId, String name, String code) {

        if (hostelId <= 0) {
            throw new IllegalArgumentException("Select a hostel.");
        }

        if (name == null || name.isBlank()
                || name.trim().length() > 80) {
            throw new IllegalArgumentException(
                    "Block name is required and must be under 80 characters."
            );
        }

        if (code == null || code.isBlank()
                || code.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "Block code is required and must be under 30 characters."
            );
        }
    }
}