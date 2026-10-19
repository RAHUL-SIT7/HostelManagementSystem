package com.hostelmanagement.ui;

import com.hostelmanagement.dao.FeePaymentDAO.FeeOverview;
import com.hostelmanagement.dao.FeePaymentDAO.PaymentHistoryRow;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.FeePaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class StudentFeesFrame extends JFrame {

    private final User user;
    private final FeePaymentService service = new FeePaymentService();

    private final JTextField monthField =
            new JTextField(YearMonth.now().toString(), 10);

    private final JTextField amountField = new JTextField(15);
    private final JTextField transactionField = new JTextField(20);

    private final JTextField paymentDateField =
            new JTextField(LocalDate.now().toString(), 12);

    private final JTextArea summaryArea = new JTextArea(7, 50);

    private final DefaultTableModel historyModel =
            new DefaultTableModel(
                    new Object[]{
                            "Billing Month",
                            "Amount",
                            "Transaction ID",
                            "Payment Date",
                            "Status",
                            "Admin Remarks",
                            "Submitted At"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable historyTable = new JTable(historyModel);
    private final JButton submitButton = new JButton("Submit Payment");

    private FeeOverview currentOverview;

    public StudentFeesFrame(User user) {
        this.user = user;

        setTitle("Hostel Management - Fees & Payments");
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "My Fees & Payments",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JButton loadMonthButton = new JButton("Load Month");
        loadMonthButton.addActionListener(e -> refreshData());

        JPanel monthPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
        monthPanel.add(new JLabel("Billing Month (YYYY-MM):"));
        monthPanel.add(monthField);
        monthPanel.add(loadMonthButton);

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "Monthly Fee Summary"
                )
        );

        historyTable.setAutoCreateRowSorter(true);
        historyTable.setRowHeight(24);
        historyTable.setFillsViewportHeight(true);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(
                BorderFactory.createTitledBorder("Payment History")
        );

        JPanel paymentForm = new JPanel(new GridBagLayout());
        paymentForm.setBorder(
                BorderFactory.createTitledBorder(
                        "Submit a Payment for Verification"
                )
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addFormRow(
                paymentForm, gbc, 0,
                "Amount:", amountField
        );

        addFormRow(
                paymentForm, gbc, 1,
                "Transaction ID:", transactionField
        );

        addFormRow(
                paymentForm, gbc, 2,
                "Payment Date (YYYY-MM-DD):", paymentDateField
        );

        submitButton.addActionListener(e -> submitPayment());

        JPanel formButtons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        formButtons.add(submitButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        paymentForm.add(formButtons, gbc);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(monthPanel, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(summaryScroll, BorderLayout.NORTH);
        center.add(historyScroll, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);
        content.add(paymentForm, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JComponent input
    ) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(input, gbc);
    }

    private YearMonth selectedMonth() {
        try {
            return YearMonth.parse(monthField.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Enter the billing month in YYYY-MM format, "
                            + "for example 2026-10."
            );
        }
    }

    private void refreshData() {
        try {
            YearMonth month = selectedMonth();

            currentOverview = service.getOverview(
                    user.getUserId(),
                    month
            );

            displaySummary(month, currentOverview);
            loadPaymentHistory();

            boolean canSubmit =
                    currentOverview.hasAllocation()
                    && "ACTIVE".equalsIgnoreCase(
                            currentOverview.getAccountStatus()
                    )
                    && currentOverview
                            .getRemainingSubmittableAmount()
                            .compareTo(BigDecimal.ZERO) > 0;

            submitButton.setEnabled(canSubmit);

        } catch (IllegalArgumentException | SQLException e) {
            currentOverview = null;
            submitButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load fee information:\n" + e.getMessage(),
                    "Fees Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void displaySummary(
            YearMonth month,
            FeeOverview overview
    ) {
        summaryArea.setText(
                "Student: " + overview.getFullName() + "\n"
                + "Account Status: "
                + overview.getAccountStatus() + "\n"
                + "Billing Month: " + month + "\n"
                + "Room Type: "
                + displayValue(overview.getRoomTypeName()) + "\n"
                + "Monthly Fee: ₹"
                + overview.getMonthlyFee().toPlainString() + "\n"
                + "Approved Payments: ₹"
                + overview.getApprovedAmount().toPlainString() + "\n"
                + "Payments Awaiting Review: ₹"
                + overview.getPendingAmount().toPlainString() + "\n"
                + "Outstanding After Approved Payments: ₹"
                + overview.getOutstandingAmount().toPlainString() + "\n"
                + "Additional Amount You Can Submit: ₹"
                + overview.getRemainingSubmittableAmount().toPlainString() + "\n"
                + "Fee Status: " + determineFeeStatus(overview) + "\n\n"
                + (overview.hasAllocation()
                    ? "Your room allocation is active."
                    : "No active room allocation. Contact the Admin.")
                + "\n"
                + "Important: Pending payments are not counted as paid. "
                + "After Admin approval, click Load Month to refresh the due."
        );

        summaryArea.setCaretPosition(0);
    }

    private String determineFeeStatus(FeeOverview overview) {
        if (!overview.hasAllocation()) {
            return "NO ACTIVE ROOM ALLOCATION";
        }

        if (overview.getMonthlyFee().compareTo(BigDecimal.ZERO) <= 0) {
            return "FEE NOT CONFIGURED";
        }

        if (overview.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
            return "PAID";
        }

        if (overview.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIALLY PAID";
        }

        if (overview.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "PAYMENT AWAITING ADMIN REVIEW";
        }

        return "DUE";
    }

    private void loadPaymentHistory() throws SQLException {
        historyModel.setRowCount(0);

        List<PaymentHistoryRow> history =
                service.getPaymentHistory(user.getUserId());

        for (PaymentHistoryRow payment : history) {
            historyModel.addRow(new Object[]{
                    payment.getBillingMonth(),
                    payment.getAmount(),
                    payment.getTransactionId(),
                    payment.getPaymentDate(),
                    payment.getStatus(),
                    displayValue(payment.getRemarks()),
                    payment.getSubmittedAt()
            });
        }
    }

    private void submitPayment() {
        try {
            if (currentOverview == null) {
                refreshData();

                if (currentOverview == null) {
                    return;
                }
            }

            YearMonth month = selectedMonth();

            BigDecimal amount = new BigDecimal(
                    amountField.getText().trim()
            );

            String transactionId = transactionField.getText().trim();

            LocalDate paymentDate = LocalDate.parse(
                    paymentDateField.getText().trim()
            );

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Submit payment for " + month + "?\n"
                            + "Amount: ₹" + amount.toPlainString() + "\n"
                            + "Transaction ID: " + transactionId + "\n\n"
                            + "The payment will be marked PENDING "
                            + "until the Admin verifies it.",
                    "Confirm Payment Submission",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            service.submitPayment(
                    user.getUserId(),
                    month,
                    amount,
                    transactionId,
                    paymentDate
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Payment submitted successfully.\n"
                            + "Status: PENDING ADMIN VERIFICATION"
            );

            amountField.setText("");
            transactionField.setText("");
            paymentDateField.setText(LocalDate.now().toString());

            refreshData();

        } catch (IllegalArgumentException | DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Check the amount, transaction ID, billing month "
                            + "and payment date.\n" + e.getMessage(),
                    "Invalid Payment Details",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Payment submission failed:\n" + e.getMessage()
                            + "\nCheck whether the transaction ID "
                            + "has already been used.",
                    "Payment Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}