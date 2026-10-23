package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class AdminPaymentDAO {

    public static class PaymentRecord {
        private final long paymentId;
        private final long studentId;
        private final String studentName;
        private final String email;
        private final YearMonth billingMonth;
        private final java.math.BigDecimal amount;
        private final String transactionId;
        private final LocalDate paymentDate;
        private final String status;
        private final String remarks;
        private final String submittedAt;

        public PaymentRecord(
                long paymentId,
                long studentId,
                String studentName,
                String email,
                YearMonth billingMonth,
                java.math.BigDecimal amount,
                String transactionId,
                LocalDate paymentDate,
                String status,
                String remarks,
                String submittedAt
        ) {
            this.paymentId = paymentId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.email = email;
            this.billingMonth = billingMonth;
            this.amount = amount;
            this.transactionId = transactionId;
            this.paymentDate = paymentDate;
            this.status = status;
            this.remarks = remarks;
            this.submittedAt = submittedAt;
        }

        public long getPaymentId() {
            return paymentId;
        }

        public long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getEmail() {
            return email;
        }

        public YearMonth getBillingMonth() {
            return billingMonth;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public LocalDate getPaymentDate() {
            return paymentDate;
        }

        public String getStatus() {
            return status;
        }

        public String getRemarks() {
            return remarks;
        }

        public String getSubmittedAt() {
            return submittedAt;
        }
    }

    public List<PaymentRecord> getPayments(String statusFilter)
            throws SQLException {

        String sql = """
                SELECT
                    p.payment_id,
                    p.student_id,
                    s.full_name,
                    u.email,
                    p.billing_month,
                    p.amount,
                    p.transaction_id,
                    p.payment_date,
                    p.payment_status,
                    p.admin_remarks,
                    p.submitted_at

                FROM payments p
                JOIN students s
                    ON s.student_id = p.student_id
                JOIN users u
                    ON u.user_id = s.user_id

                WHERE (? = 'ALL' OR p.payment_status = ?)

                ORDER BY
                    CASE
                        WHEN p.payment_status = 'PENDING' THEN 0
                        ELSE 1
                    END,
                    p.submitted_at DESC
                """;

        List<PaymentRecord> records = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, statusFilter);
            ps.setString(2, statusFilter);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date billingDate = rs.getDate("billing_month");
                    Date paymentDate = rs.getDate("payment_date");
                    java.sql.Timestamp submitted =
                            rs.getTimestamp("submitted_at");

                    records.add(new PaymentRecord(
                            rs.getLong("payment_id"),
                            rs.getLong("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            YearMonth.from(billingDate.toLocalDate()),
                            rs.getBigDecimal("amount"),
                            rs.getString("transaction_id"),
                            paymentDate.toLocalDate(),
                            rs.getString("payment_status"),
                            rs.getString("admin_remarks"),
                            submitted == null ? "" : submitted.toString()
                    ));
                }
            }
        }

        return records;
    }

    public void reviewPayment(
            long adminUserId,
            long paymentId,
            String newStatus,
            String remarks
    ) throws SQLException {

        if (adminUserId <= 0 || paymentId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid Admin or payment ID."
            );
        }

        if (!"APPROVED".equals(newStatus)
                && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Review status must be APPROVED or REJECTED."
            );
        }

        remarks = remarks == null ? "" : remarks.trim();

        if (remarks.length() > 500) {
            throw new IllegalArgumentException(
                    "Admin remarks cannot exceed 500 characters."
            );
        }

        if ("REJECTED".equals(newStatus) && remarks.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter a reason for rejecting the payment."
            );
        }

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Verify the currently logged-in Admin.
                String adminSql = """
                        SELECT user_id
                        FROM users
                        WHERE user_id = ?
                          AND role = 'ADMIN'
                          AND account_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(adminSql)) {
                    ps.setLong(1, adminUserId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Only an active Admin can review payments."
                            );
                        }
                    }
                }

                // Only pending payments can be reviewed once.
                String updateSql = """
                        UPDATE payments
                        SET payment_status = ?,
                            reviewed_by = ?,
                            reviewed_at = CURRENT_TIMESTAMP,
                            admin_remarks = ?
                        WHERE payment_id = ?
                          AND payment_status = 'PENDING'
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus);
                    ps.setLong(2, adminUserId);

                    if (remarks.isBlank()) {
                        ps.setNull(3, Types.VARCHAR);
                    } else {
                        ps.setString(3, remarks);
                    }

                    ps.setLong(4, paymentId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Payment was not found or has already been reviewed. "
                                        + "Refresh the list and try again."
                        );
                    }
                }

                con.commit();

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
}