package com.hostelmanagement.service;

import com.hostelmanagement.dao.PendingApplicationDAO;
import com.hostelmanagement.model.AvailableBed;
import com.hostelmanagement.model.PendingApplication;

import java.sql.SQLException;
import java.util.List;

public class PendingApplicationService {

    private final PendingApplicationDAO dao =
            new PendingApplicationDAO();

    public List<PendingApplication> getPendingApplications()
            throws SQLException {
        return dao.getPendingApplications();
    }

    public List<AvailableBed> getAvailableBeds(long studentId)
            throws SQLException {

        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID.");
        }

        return dao.getAvailableBeds(studentId);
    }

    public void approveAndAllocate(
            long adminUserId,
            long userId,
            long studentId,
            long bedId
    ) throws SQLException {

        if (adminUserId <= 0 || userId <= 0
                || studentId <= 0 || bedId <= 0) {
            throw new IllegalArgumentException(
                    "Admin, student and bed details must be valid."
            );
        }

        dao.approveAndAllocate(
                adminUserId,
                userId,
                studentId,
                bedId
        );
    }

    public void rejectApplication(
            long adminUserId,
            long userId,
            long studentId
    ) throws SQLException {

        if (adminUserId <= 0 || userId <= 0 || studentId <= 0) {
            throw new IllegalArgumentException(
                    "Admin and student details must be valid."
            );
        }

        dao.rejectApplication(adminUserId, userId, studentId);
    }
}