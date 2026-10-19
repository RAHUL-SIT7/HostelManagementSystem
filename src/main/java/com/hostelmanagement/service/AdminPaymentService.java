package com.hostelmanagement.service;

import com.hostelmanagement.dao.AdminPaymentDAO;
import com.hostelmanagement.dao.AdminPaymentDAO.PaymentRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class AdminPaymentService {

    private final AdminPaymentDAO dao = new AdminPaymentDAO();

    private static final Set<String> ALLOWED_FILTERS = Set.of(
            "ALL", "PENDING", "APPROVED", "REJECTED"
    );

    public List<PaymentRecord> getPayments(String statusFilter)
            throws SQLException {

        if (statusFilter == null
                || !ALLOWED_FILTERS.contains(statusFilter)) {
            throw new IllegalArgumentException(
                    "Invalid payment status filter."
            );
        }

        return dao.getPayments(statusFilter);
    }

    public void reviewPayment(
            long adminUserId,
            long paymentId,
            String newStatus,
            String remarks
    ) throws SQLException {

        dao.reviewPayment(
                adminUserId,
                paymentId,
                newStatus,
                remarks
        );
    }
}