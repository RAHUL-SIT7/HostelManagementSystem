package com.hostelmanagement.service;

import com.hostelmanagement.dao.ComplaintDAO;
import com.hostelmanagement.dao.ComplaintDAO.ComplaintRecord;
import com.hostelmanagement.dao.ComplaintDAO.RoomInfo;

import java.sql.SQLException;
import java.util.List;

public class ComplaintService {
    private final ComplaintDAO dao = new ComplaintDAO();

    public RoomInfo getCurrentRoom(long studentUserId) throws SQLException {
        return dao.getCurrentRoom(studentUserId);
    }

    public List<String> getCategories(long studentUserId) throws SQLException {
        return dao.getCategories(studentUserId);
    }

    public List<String> getStatuses(long adminUserId) throws SQLException {
        return dao.getStatuses(adminUserId);
    }

    public long submitComplaint(long studentUserId, String category,
                                String otherProfessional, String description) throws SQLException {
        return dao.submitComplaint(studentUserId, category, otherProfessional, description);
    }

    public List<ComplaintRecord> getStudentComplaints(long studentUserId) throws SQLException {
        return dao.getStudentComplaints(studentUserId);
    }

    public List<ComplaintRecord> getAllComplaints(long adminUserId) throws SQLException {
        return dao.getAllComplaints(adminUserId);
    }

    public void updateComplaint(long adminUserId, long complaintId,
                                String status, String resolutionNote) throws SQLException {
        dao.updateComplaint(adminUserId, complaintId, status, resolutionNote);
    }
}
