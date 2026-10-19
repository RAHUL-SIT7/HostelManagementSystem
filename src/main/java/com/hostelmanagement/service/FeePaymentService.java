package com.hostelmanagement.service;

import com.hostelmanagement.dao.FeePaymentDAO;
import com.hostelmanagement.dao.FeePaymentDAO.FeeOverview;
import com.hostelmanagement.dao.FeePaymentDAO.PaymentHistoryRow;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class FeePaymentService {

    private final FeePaymentDAO dao = new FeePaymentDAO();

    public FeeOverview getOverview(long userId, YearMonth month)
            throws SQLException {

        if (userId <= 0 || month == null) {
            throw new IllegalArgumentException(
                    "Invalid student or billing month."
            );
        }

        return dao.getOverview(userId, month);
    }

    public List<PaymentHistoryRow> getPaymentHistory(long userId)
            throws SQLException {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid student account.");
        }

        return dao.getPaymentHistory(userId);
    }

    public void submitPayment(
            long userId,
            YearMonth billingMonth,
            BigDecimal amount,
            String transactionId,
            LocalDate paymentDate
    ) throws SQLException {

        if (billingMonth == null || amount == null
                || transactionId == null || paymentDate == null) {
            throw new IllegalArgumentException(
                    "Please complete all required payment fields."
            );
        }

        dao.submitPayment(
                userId,
                billingMonth,
                amount,
                transactionId,
                paymentDate
        );
    }
}