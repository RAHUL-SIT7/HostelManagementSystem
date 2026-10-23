package com.hostelmanagement.ui;

import com.hostelmanagement.dao.AdminPaymentDAO.PaymentRecord;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.AdminPaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Admin payment-verification screen. Payment queries and review updates are
 * executed with SwingWorker so the Swing Event Dispatch Thread stays responsive.
 */
public class AdminPaymentsFrame extends JFrame {

    private final User adminUser;
    private final AdminPaymentService service = new AdminPaymentService();
    private final List<PaymentRecord> records = new ArrayList<>();

    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"PENDING", "ALL", "APPROVED", "REJECTED"}
    );

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "Payment ID",
                    "Student ID",
                    "Student Name",
                    "Email",
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

    private final JTable paymentTable = new JTable(tableModel);
    private final JTextArea detailsArea = new JTextArea(6, 40);
    private final JTextField remarksField = new JTextField(30);
    private final JLabel countLabel = new JLabel("Payments: 0");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton approveButton = new JButton("Approve Payment");
    private final JButton rejectButton = new JButton("Reject Payment");

    /** Prevents overlapping loads/reviews from repeated clicks. */
    private boolean paymentOperationInProgress;

    public AdminPaymentsFrame(User adminUser) {
        this.adminUser = adminUser;

        setTitle("Hostel Management - Admin Payment Verification");
        setSize(1350, 750);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadPayments();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Payment Verification", SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        refreshButton.addActionListener(e -> loadPayments());

        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.add(title, BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Payment Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(refreshButton);
        header.add(filterPanel, BorderLayout.EAST);

        statusFilter.addActionListener(e -> loadPayments());

        paymentTable.setAutoCreateRowSorter(true);
        paymentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentTable.setRowHeight(25);
        paymentTable.setFillsViewportHeight(true);
        paymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        paymentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedPayment();
            }
        });

        JScrollPane tableScroll = new JScrollPane(paymentTable);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder("Student Payment Records")
        );

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(
                BorderFactory.createTitledBorder("Selected Payment")
        );

        JPanel reviewPanel = new JPanel(new GridBagLayout());
        reviewPanel.setBorder(BorderFactory.createTitledBorder("Admin Review"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        reviewPanel.add(new JLabel("Admin Remarks:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        reviewPanel.add(remarksField, gbc);

        approveButton.addActionListener(e -> reviewSelectedPayment("APPROVED"));
        rejectButton.addActionListener(e -> reviewSelectedPayment("REJECTED"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(approveButton);
        buttons.add(rejectButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        reviewPanel.add(buttons, gbc);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(countLabel, BorderLayout.WEST);
        footer.add(
                new JLabel(
                        "Verify the transaction against your bank/UPI records before approving.",
                        SwingConstants.RIGHT
                ),
                BorderLayout.EAST
        );

        JPanel lowerPanel = new JPanel(new BorderLayout(8, 8));
        lowerPanel.add(detailsScroll, BorderLayout.CENTER);
        lowerPanel.add(reviewPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, tableScroll, lowerPanel
        );
        split.setResizeWeight(0.60);
        split.setDividerLocation(360);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
    }

    /** Loads payment rows in the background and updates Swing components on the EDT. */
    private void loadPayments() {
        if (paymentOperationInProgress) {
            return;
        }

        String filter = (String) statusFilter.getSelectedItem();
        paymentOperationInProgress = true;
        setBusy(true);
        countLabel.setText("Loading payments...");

        SwingWorker<List<PaymentRecord>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PaymentRecord> doInBackground() throws Exception {
                // JDBC query runs off the Swing Event Dispatch Thread.
                return service.getPayments(filter);
            }

            @Override
            protected void done() {
                paymentOperationInProgress = false;
                setBusy(false);

                try {
                    List<PaymentRecord> loadedRecords = get();
                    records.clear();
                    records.addAll(loadedRecords);
                    tableModel.setRowCount(0);

                    for (PaymentRecord payment : records) {
                        tableModel.addRow(new Object[]{
                                payment.getPaymentId(),
                                payment.getStudentId(),
                                payment.getStudentName(),
                                payment.getEmail(),
                                payment.getBillingMonth(),
                                payment.getAmount(),
                                payment.getTransactionId(),
                                payment.getPaymentDate(),
                                payment.getStatus(),
                                payment.getRemarks() == null ? "" : payment.getRemarks(),
                                payment.getSubmittedAt()
                        });
                    }

                    countLabel.setText("Payments: " + records.size());
                    if (records.isEmpty()) {
                        detailsArea.setText(
                                "No payment records exist for this filter."
                        );
                    } else {
                        showSelectedPayment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showLoadError(e);
                } catch (ExecutionException e) {
                    showLoadError(e.getCause() == null ? e : e.getCause());
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
        statusFilter.setEnabled(!busy);
        approveButton.setEnabled(!busy);
        rejectButton.setEnabled(!busy);
        paymentTable.setEnabled(!busy);
        remarksField.setEnabled(!busy);
    }

    private void showLoadError(Throwable cause) {
        countLabel.setText("Could not load payments");
        showError("Could not load payments:\n" + messageOf(cause));
    }

    private PaymentRecord getSelectedPayment() {
        int selectedRow = paymentTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }

        int modelRow = paymentTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= records.size()) {
            return null;
        }
        return records.get(modelRow);
    }

    private void showSelectedPayment() {
        PaymentRecord payment = getSelectedPayment();
        if (payment == null) {
            detailsArea.setText("");
            remarksField.setText("");
            return;
        }

        detailsArea.setText(
                "Student: " + payment.getStudentName() + "\n"
                + "Student ID: " + payment.getStudentId() + "\n"
                + "Email: " + payment.getEmail() + "\n"
                + "Billing Month: " + payment.getBillingMonth() + "\n"
                + "Amount: ₹" + payment.getAmount() + "\n"
                + "Transaction ID: " + payment.getTransactionId() + "\n"
                + "Payment Date: " + payment.getPaymentDate() + "\n"
                + "Status: " + payment.getStatus() + "\n"
                + "Admin Remarks: "
                + (payment.getRemarks() == null ? "—" : payment.getRemarks()) + "\n"
                + "Submitted At: " + payment.getSubmittedAt()
        );
        detailsArea.setCaretPosition(0);
        remarksField.setText(
                payment.getRemarks() == null ? "" : payment.getRemarks()
        );
    }

    private void reviewSelectedPayment(String newStatus) {
        if (paymentOperationInProgress) {
            return;
        }

        PaymentRecord payment = getSelectedPayment();
        if (payment == null) {
            showError("Select a payment record first.");
            return;
        }

        if (!"PENDING".equals(payment.getStatus())) {
            showError(
                    "This payment has already been reviewed. "
                            + "Select a pending payment instead."
            );
            return;
        }

        String remarks = remarksField.getText().trim();
        if ("REJECTED".equals(newStatus) && remarks.isBlank()) {
            showError("Enter a reason in Admin Remarks before rejecting.");
            return;
        }

        String action = "APPROVED".equals(newStatus) ? "approve" : "reject";
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to " + action + " this payment?\n\n"
                        + "Student: " + payment.getStudentName() + "\n"
                        + "Amount: ₹" + payment.getAmount() + "\n"
                        + "Transaction ID: " + payment.getTransactionId()
                        + "\n\n"
                        + "Confirm that you have checked the actual "
                        + "transaction against your bank/UPI records.",
                "Confirm Payment Review",
                JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        long paymentId = payment.getPaymentId();
        long reviewerId = adminUser.getUserId();
        paymentOperationInProgress = true;
        setBusy(true);
        countLabel.setText("Updating payment...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // The review UPDATE and DAO transaction run off the EDT.
                service.reviewPayment(
                        reviewerId, paymentId, newStatus, remarks
                );
                return null;
            }

            @Override
            protected void done() {
                paymentOperationInProgress = false;
                setBusy(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AdminPaymentsFrame.this,
                            "Payment " + newStatus + " successfully."
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Could not review payment:\n" + messageOf(e));
                } catch (ExecutionException e) {
                    showError(
                            "Could not review payment:\n"
                                    + messageOf(e.getCause() == null ? e : e.getCause())
                    );
                } finally {
                    // Reload from MySQL after the review attempt.
                    loadPayments();
                }
            }
        };
        worker.execute();
    }

    private String messageOf(Throwable cause) {
        if (cause == null || cause.getMessage() == null
                || cause.getMessage().isBlank()) {
            return "An unexpected error occurred.";
        }
        return cause.getMessage();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Payment Verification",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
