package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class FeePaymentDAO {

    public static final class FeeOverview {
        private final long studentId;
        private final String fullName;
        private final String accountStatus;
        private final String roomTypeName;
        private final BigDecimal monthlyFee;
        private final BigDecimal approvedAmount;
        private final BigDecimal pendingAmount;
        private final boolean allocated;

        public FeeOverview(
                long studentId,
                String fullName,
                String accountStatus,
                String roomTypeName,
                BigDecimal monthlyFee,
                BigDecimal approvedAmount,
                BigDecimal pendingAmount,
                boolean allocated
        ) {
            this.studentId = studentId;
            this.fullName = fullName;
            this.accountStatus = accountStatus;
            this.roomTypeName = roomTypeName;
            this.monthlyFee = monthlyFee;
            this.approvedAmount = approvedAmount;
            this.pendingAmount = pendingAmount;
            this.allocated = allocated;
        }

        public long getStudentId() { return studentId; }
        public String getFullName() { return fullName; }
        public String getAccountStatus() { return accountStatus; }
        public String getRoomTypeName() { return roomTypeName; }
        public BigDecimal getMonthlyFee() { return monthlyFee; }
        public BigDecimal getApprovedAmount() { return approvedAmount; }
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public boolean hasAllocation() { return allocated; }

        public BigDecimal getOutstandingAmount() {
            return monthlyFee.subtract(approvedAmount)
                    .max(BigDecimal.ZERO);
        }

        public BigDecimal getRemainingSubmittableAmount() {
            return monthlyFee.subtract(approvedAmount)
                    .subtract(pendingAmount)
                    .max(BigDecimal.ZERO);
        }
    }

    public static final class PaymentHistoryRow {
        private final long paymentId;
        private final YearMonth billingMonth;
        private final BigDecimal amount;
        private final String transactionId;
        private final LocalDate paymentDate;
        private final String status;
        private final String remarks;
        private final String submittedAt;

        public PaymentHistoryRow(
                long paymentId,
                YearMonth billingMonth,
                BigDecimal amount,
                String transactionId,
                LocalDate paymentDate,
                String status,
                String remarks,
                String submittedAt
        ) {
            this.paymentId = paymentId;
            this.billingMonth = billingMonth;
            this.amount = amount;
            this.transactionId = transactionId;
            this.paymentDate = paymentDate;
            this.status = status;
            this.remarks = remarks;
            this.submittedAt = submittedAt;
        }

        public long getPaymentId() { return paymentId; }
        public YearMonth getBillingMonth() { return billingMonth; }
        public BigDecimal getAmount() { return amount; }
        public String getTransactionId() { return transactionId; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
        public String getSubmittedAt() { return submittedAt; }
    }

    public FeeOverview getOverview(long userId, YearMonth billingMonth)
            throws SQLException {

        if (userId <= 0 || billingMonth == null) {
            throw new IllegalArgumentException(
                    "A valid student account and billing month are required."
            );
        }

        String sql = """
                SELECT
                    s.student_id,
                    s.full_name,
                    u.account_status,
                    rt.type_name AS room_type_name,
                    rt.default_monthly_fee,
                    fp.monthly_amount

                FROM users u
                JOIN students s
                    ON s.user_id = u.user_id

                LEFT JOIN room_allocations ra
                    ON ra.student_id = s.student_id
                   AND ra.allocation_status = 'ACTIVE'

                LEFT JOIN beds b
                    ON b.bed_id = ra.bed_id

                LEFT JOIN rooms r
                    ON r.room_id = b.room_id

                LEFT JOIN room_types rt
                    ON rt.room_type_id = r.room_type_id

                LEFT JOIN fee_plans fp
                    ON fp.room_type_id = rt.room_type_id
                   AND fp.effective_from <= ?
                   AND (
                       fp.effective_to IS NULL
                       OR fp.effective_to >= ?
                   )

                WHERE u.user_id = ?
                  AND u.role = 'STUDENT'

                ORDER BY ra.allocated_at DESC, fp.effective_from DESC
                LIMIT 1
                """;

        long studentId;
        String fullName;
        String status;
        String roomType;
        BigDecimal monthlyFee;
        boolean allocated;

        Date monthStart = Date.valueOf(billingMonth.atDay(1));

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, monthStart);
            ps.setDate(2, monthStart);
            ps.setLong(3, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Student account could not be found."
                    );
                }

                studentId = rs.getLong("student_id");
                fullName = rs.getString("full_name");
                status = rs.getString("account_status");
                roomType = rs.getString("room_type_name");

                allocated = roomType != null;

                monthlyFee = rs.getBigDecimal("monthly_amount");

                if (monthlyFee == null) {
                    monthlyFee = rs.getBigDecimal("default_monthly_fee");
                }

                if (monthlyFee == null || !allocated) {
                    monthlyFee = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal approved = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;

        String totalsSql = """
                SELECT
                    COALESCE(
                        SUM(CASE
                            WHEN payment_status = 'APPROVED'
                            THEN amount ELSE 0
                        END), 0
                    ) AS approved_amount,

                    COALESCE(
                        SUM(CASE
                            WHEN payment_status = 'PENDING'
                            THEN amount ELSE 0
                        END), 0
                    ) AS pending_amount

                FROM payments
                WHERE student_id = ?
                  AND billing_month = ?
                """;

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(totalsSql)) {

            ps.setLong(1, studentId);
            ps.setDate(2, monthStart);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    approved = rs.getBigDecimal("approved_amount");
                    pending = rs.getBigDecimal("pending_amount");
                }
            }
        }

        return new FeeOverview(
                studentId,
                fullName,
                status,
                roomType,
                monthlyFee,
                approved,
                pending,
                allocated
        );
    }

    public List<PaymentHistoryRow> getPaymentHistory(long userId)
            throws SQLException {

        String sql = """
                SELECT
                    p.payment_id,
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

                WHERE s.user_id = ?

                ORDER BY p.billing_month DESC, p.submitted_at DESC
                """;

        List<PaymentHistoryRow> result = new ArrayList<>();

        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date billingDate = rs.getDate("billing_month");
                    Date paymentDate = rs.getDate("payment_date");
                    Timestamp submitted = rs.getTimestamp("submitted_at");

                    result.add(new PaymentHistoryRow(
                            rs.getLong("payment_id"),
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

        return result;
    }

    public void submitPayment(
            long userId,
            YearMonth billingMonth,
            BigDecimal amount,
            String transactionId,
            LocalDate paymentDate
    ) throws SQLException {

        if (userId <= 0 || billingMonth == null || amount == null
                || transactionId == null || paymentDate == null) {
            throw new IllegalArgumentException(
                    "Please complete all required payment fields."
            );
        }

        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount must have at most two decimal places."
            );
        }

        transactionId = transactionId.trim();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        if (transactionId.isBlank() || transactionId.length() > 120) {
            throw new IllegalArgumentException(
                    "Enter a valid transaction ID (maximum 120 characters)."
            );
        }

        if (paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Payment date cannot be in the future."
            );
        }

        Date monthStart = Date.valueOf(billingMonth.atDay(1));

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Lock this student row so concurrent payment submissions
                // for this account are checked sequentially.
                long studentId;

                String studentSql = """
                        SELECT s.student_id
                        FROM users u
                        JOIN students s ON s.user_id = u.user_id
                        WHERE u.user_id = ?
                          AND u.role = 'STUDENT'
                          AND u.account_status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(studentSql)) {

                    ps.setLong(1, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Student account is not active."
                            );
                        }

                        studentId = rs.getLong("student_id");
                    }
                }

                // Transaction references are global identifiers. Do not allow
                // a reference to be reused, even when its earlier payment was
                // rejected. A UNIQUE index in MySQL is also required to close
                // the race between simultaneous submissions by different users.
                if (transactionIdAlreadyUsed(con, transactionId)) {
                    throw new SQLException(
                            "This transaction ID has already been submitted. "
                                    + "Each transaction reference can be used only once."
                    );
                }

                String feeSql = """
                        SELECT
                            rt.default_monthly_fee,
                            fp.fee_plan_id,
                            fp.monthly_amount

                        FROM room_allocations ra
                        JOIN beds b ON b.bed_id = ra.bed_id
                        JOIN rooms r ON r.room_id = b.room_id
                        JOIN room_types rt
                            ON rt.room_type_id = r.room_type_id

                        LEFT JOIN fee_plans fp
                            ON fp.room_type_id = rt.room_type_id
                           AND fp.effective_from <= ?
                           AND (
                               fp.effective_to IS NULL
                               OR fp.effective_to >= ?
                           )

                        WHERE ra.student_id = ?
                          AND ra.allocation_status = 'ACTIVE'

                        ORDER BY ra.allocated_at DESC,
                                 fp.effective_from DESC
                        LIMIT 1
                        """;

                Long feePlanId;
                BigDecimal monthlyFee;

                try (PreparedStatement ps =
                             con.prepareStatement(feeSql)) {

                    ps.setDate(1, monthStart);
                    ps.setDate(2, monthStart);
                    ps.setLong(3, studentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "An active room allocation is required "
                                            + "before payment submission."
                            );
                        }

                        long planId = rs.getLong("fee_plan_id");
                        feePlanId = rs.wasNull() ? null : planId;

                        monthlyFee = rs.getBigDecimal("monthly_amount");

                        if (monthlyFee == null) {
                            monthlyFee =
                                    rs.getBigDecimal("default_monthly_fee");
                        }

                        if (monthlyFee == null
                                || monthlyFee.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new SQLException(
                                    "No valid monthly fee is configured "
                                            + "for this room type."
                            );
                        }
                    }
                }

                String reservedSql = """
                        SELECT COALESCE(SUM(amount), 0) AS reserved
                        FROM payments
                        WHERE student_id = ?
                          AND billing_month = ?
                          AND payment_status IN ('APPROVED', 'PENDING')
                        """;

                BigDecimal reserved;

                try (PreparedStatement ps =
                             con.prepareStatement(reservedSql)) {

                    ps.setLong(1, studentId);
                    ps.setDate(2, monthStart);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        reserved = rs.getBigDecimal("reserved");
                    }
                }

                BigDecimal remaining =
                        monthlyFee.subtract(reserved);

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new SQLException(
                            "There is no remaining amount to submit "
                                    + "for this billing month."
                    );
                }

                if (amount.compareTo(remaining) > 0) {
                    throw new SQLException(
                            "Amount exceeds the remaining balance of "
                                    + remaining.toPlainString()
                                    + " for " + billingMonth + "."
                    );
                }

                String insertSql = """
                        INSERT INTO payments (
                            student_id,
                            fee_plan_id,
                            billing_month,
                            amount,
                            transaction_id,
                            payment_date,
                            proof_path,
                            payment_status,
                            submitted_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, NULL,
                                'PENDING', CURRENT_TIMESTAMP)
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {

                    ps.setLong(1, studentId);

                    if (feePlanId == null) {
                        ps.setNull(2, Types.BIGINT);
                    } else {
                        ps.setLong(2, feePlanId);
                    }

                    ps.setDate(3, monthStart);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, transactionId);
                    ps.setDate(6, Date.valueOf(paymentDate));

                    try {
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        if (isDuplicateKeyError(e)) {
                            throw new SQLException(
                                    "This transaction ID has already been submitted. "
                                            + "Each transaction reference can be used only once.",
                                    e.getSQLState(),
                                    e.getErrorCode(),
                                    e
                            );
                        }
                        throw e;
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

    private boolean transactionIdAlreadyUsed(Connection con, String transactionId)
            throws SQLException {
        String sql = """
                SELECT payment_id
                FROM payments
                WHERE LOWER(TRIM(transaction_id)) = LOWER(?)
                LIMIT 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, transactionId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isDuplicateKeyError(SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            if (current.getErrorCode() == 1062) { // MySQL duplicate-key error
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }
}