package com.hostelmanagement.service;

import com.hostelmanagement.dao.HostelDAO;
import com.hostelmanagement.model.Hostel;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class HostelService {

    private final HostelDAO hostelDAO = new HostelDAO();

    public List<Hostel> getAllHostels() throws SQLException {
        return hostelDAO.getAllHostels();
    }

    public void addHostel(String name, String code,
                          String address, String type)
            throws SQLException {

        validate(name, code, address, type);

        hostelDAO.addHostel(
                name.trim(),
                code.trim().toUpperCase(Locale.ROOT),
                address.trim(),
                type.trim()
        );
    }

    public void updateHostel(long id, String name, String code,
                             String address, String type)
            throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Select a hostel before updating."
            );
        }

        validate(name, code, address, type);

        hostelDAO.updateHostel(
                id,
                name.trim(),
                code.trim().toUpperCase(Locale.ROOT),
                address.trim(),
                type.trim()
        );
    }

    public void deactivateHostel(long id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Select a hostel first."
            );
        }

        hostelDAO.deactivateHostel(id);
    }

    private void validate(String name, String code,
                          String address, String type) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Hostel name is required.");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Hostel code is required.");
        }

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Hostel type is required.");
        }

        if (name.trim().length() > 120
                || code.trim().length() > 30
                || type.trim().length() > 30
                || (address != null && address.trim().length() > 255)) {
            throw new IllegalArgumentException(
                    "One or more fields exceed their allowed length."
            );
        }
    }
}