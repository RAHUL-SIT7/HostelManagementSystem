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
import java.util.concurrent.ExecutionException;

/**
 * Student fee screen. Database work is performed with SwingWorker so JDBC
 * calls do not block Swing's Event Dispatch Thread (EDT).
 */
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
    private final JLabel activityLabel = new JLabel(" ");
    private final JButton loadMonthButton = new JButton("Load Month");
    private final JButton submitButton = new JButton("Submit Payment");

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

    private FeeOverview currentOverview;
    private YearMonth displayedMonth;
    private boolean paymentSubmissionInProgress;

    private record FeeScreenData(
            FeeOverview overview,
            List<PaymentHistoryRow> history
    ) { }

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

        loadMonthButton.addActionListener(e -> refreshData());

        JPanel monthPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
        monthPanel.add(new JLabel("Billing Month (YYYY-MM):"));
        monthPanel.add(monthField);
        monthPanel.add(loadMonthButton);
        monthPanel.add(activityLabel);

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(
                BorderFactory.createTitledBorder("Monthly Fee Summary")
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

        addFormRow(paymentForm, gbc, 0, "Amount:", amountField);
        addFormRow(paymentForm, gbc, 1, "Transaction ID:", transactionField);
        addFormRow(
                paymentForm, gbc, 2,
                "Payment Date (YYYY-MM-DD):", paymentDateField
        );

        submitButton.addActionListener(e -> submitPayment());

        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
                            + "for example 2026-10.", e
            );
        }
    }

    /** Loads the summary and history on a worker thread instead of the EDT. */
    private void refreshData() {
        if (paymentSubmissionInProgress) {
            return;
        }

        final YearMonth month;
        try {
            month = selectedMonth();
        } catch (IllegalArgumentException e) {
            currentOverview = null;
            displayedMonth = null;
            submitButton.setEnabled(false);
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Invalid Billing Month",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        setLoadingState(true, "Loading fee information...");

        SwingWorker<FeeScreenData, Void> worker = new SwingWorker<>() {//thread-safe background worker for JDBC calls
            @Override
            protected FeeScreenData doInBackground() throws Exception {
                // JDBC queries run on this background worker thread.
                FeeOverview overview = service.getOverview(
                        user.getUserId(), month
                );
                List<PaymentHistoryRow> history =
                        service.getPaymentHistory(user.getUserId());
                return new FeeScreenData(overview, history);
            }

            @Override
            protected void done() {
                setLoadingState(false, "");
                try {
                    FeeScreenData data = get();
                    currentOverview = data.overview();
                    displayedMonth = month;
                    displaySummary(month, currentOverview);
                    displayPaymentHistory(data.history());

                    boolean canSubmit = currentOverview.hasAllocation()
                            && "ACTIVE".equalsIgnoreCase(
                                    currentOverview.getAccountStatus()
                            )
                            && currentOverview
                                    .getRemainingSubmittableAmount()
                                    .compareTo(BigDecimal.ZERO) > 0;
                    submitButton.setEnabled(canSubmit);
                    activityLabel.setText("Fee data loaded");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleLoadFailure(e);
                } catch (ExecutionException e) {
                    handleLoadFailure(e.getCause() == null ? e : e.getCause());
                }
            }
        };
        worker.execute();
    }

    private void setLoadingState(boolean loading, String message) {
        loadMonthButton.setEnabled(!loading && !paymentSubmissionInProgress);
        monthField.setEnabled(!loading && !paymentSubmissionInProgress);
        submitButton.setEnabled(!loading && !paymentSubmissionInProgress
                && currentOverview != null);
        activityLabel.setText(message);
    }

    private void handleLoadFailure(Throwable cause) {
        currentOverview = null;
        displayedMonth = null;
        submitButton.setEnabled(false);
        activityLabel.setText("Unable to load fee data");
        JOptionPane.showMessageDialog(
                this,
                "Unable to load fee information:\n" + messageOf(cause),
                "Fees Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private String messageOf(Throwable cause) {
        if (cause == null || cause.getMessage() == null
                || cause.getMessage().isBlank()) {
            return "An unexpected error occurred.";
        }
        return cause.getMessage();
    }

    private void displaySummary(YearMonth month, FeeOverview overview) {
        summaryArea.setText(
                "Student: " + overview.getFullName() + "\n"
                + "Account Status: " + overview.getAccountStatus() + "\n"
                + "Billing Month: " + month + "\n"
                + "Room Type: " + displayValue(overview.getRoomTypeName()) + "\n"
                + "Monthly Fee: ₹" + overview.getMonthlyFee().toPlainString() + "\n"
                + "Approved Payments: ₹" + overview.getApprovedAmount().toPlainString() + "\n"
                + "Payments Awaiting Review: ₹" + overview.getPendingAmount().toPlainString() + "\n"
                + "Outstanding After Approved Payments: ₹" + overview.getOutstandingAmount().toPlainString() + "\n"
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

    private void displayPaymentHistory(List<PaymentHistoryRow> history) {
        historyModel.setRowCount(0);
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
        if (paymentSubmissionInProgress) {
            return;
        }

        if (currentOverview == null || displayedMonth == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Load fee information before submitting a payment.",
                    "Fee Information Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final YearMonth month;
        final BigDecimal amount;
        final String transactionId;
        final LocalDate paymentDate;

        try {
            month = selectedMonth();
            if (!month.equals(displayedMonth)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Click Load Month and wait for the fee summary to refresh "
                                + "before submitting for a different month.",
                        "Refresh Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            amount = new BigDecimal(amountField.getText().trim());
            transactionId = transactionField.getText().trim();
            paymentDate = LocalDate.parse(paymentDateField.getText().trim());
        } catch (IllegalArgumentException | DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Check the amount, transaction ID, billing month "
                            + "and payment date.\n" + e.getMessage(),
                    "Invalid Payment Details",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

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

        paymentSubmissionInProgress = true;
        setSubmissionControlsEnabled(false);
        activityLabel.setText("Submitting payment in background...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // The JDBC transaction is executed off the Swing EDT.
                service.submitPayment(
                        user.getUserId(), month, amount,
                        transactionId, paymentDate
                );
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            StudentFeesFrame.this,
                            "Payment submitted successfully.\n"
                                    + "Status: PENDING ADMIN VERIFICATION"
                    );
                    amountField.setText("");
                    transactionField.setText("");
                    paymentDateField.setText(LocalDate.now().toString());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showSubmissionError(e);
                } catch (ExecutionException e) {
                    showSubmissionError(
                            e.getCause() == null ? e : e.getCause()
                    );
                } finally {
                    paymentSubmissionInProgress = false;
                    setSubmissionControlsEnabled(true);
                    refreshData();
                }
            }
        };
        worker.execute();
    }

    private void showSubmissionError(Throwable cause) {
        String title = cause instanceof IllegalArgumentException
                ? "Invalid Payment Details"
                : "Payment Error";
        JOptionPane.showMessageDialog(
                this,
                "Payment submission failed:\n" + messageOf(cause)
                        + (cause instanceof SQLException
                            ? "\nCheck whether the transaction ID has already been used."
                            : ""),
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void setSubmissionControlsEnabled(boolean enabled) {
        monthField.setEnabled(enabled);
        amountField.setEnabled(enabled);
        transactionField.setEnabled(enabled);
        paymentDateField.setEnabled(enabled);
        loadMonthButton.setEnabled(enabled);
        submitButton.setEnabled(enabled && currentOverview != null);
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
