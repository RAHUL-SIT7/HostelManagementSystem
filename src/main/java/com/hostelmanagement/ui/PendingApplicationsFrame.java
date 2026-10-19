package com.hostelmanagement.ui;

import com.hostelmanagement.model.AvailableBed;
import com.hostelmanagement.model.PendingApplication;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.PendingApplicationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PendingApplicationsFrame extends JFrame {

    private final User adminUser;

    private final PendingApplicationService service =
            new PendingApplicationService();

    private final List<PendingApplication> applications =
            new ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "User ID", "Student ID", "Full Name", "Email",
                    "Profession", "Preferred Type", "Preferred Room",
                    "Registered At"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable applicationTable = new JTable(tableModel);
    private final JTextArea detailsArea = new JTextArea(12, 35);
    private final JComboBox<AvailableBed> bedCombo = new JComboBox<>();
    private final JLabel countLabel =
            new JLabel("Pending applications: 0");

    public PendingApplicationsFrame(User adminUser) {
        this.adminUser = adminUser;

        setTitle("Hostel Management - Pending Applications");
        setSize(1200, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadPendingApplications();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Pending Student Applications",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JButton refreshButton = new JButton("Refresh Applications");
        refreshButton.addActionListener(e -> loadPendingApplications());

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.add(title, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);

        applicationTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        applicationTable.setAutoCreateRowSorter(true);
        applicationTable.setRowHeight(26);

        applicationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedApplication();
            }
        });

        JScrollPane tableScroll = new JScrollPane(applicationTable);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "Applications Awaiting Review"
                )
        );

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(
                BorderFactory.createTitledBorder("Applicant Details")
        );

        JButton refreshBedsButton = new JButton("Refresh Available Beds");
        JButton approveButton = new JButton("Approve & Allocate Bed");
        JButton rejectButton = new JButton("Reject Application");

        refreshBedsButton.addActionListener(e -> loadAvailableBeds());
        approveButton.addActionListener(e -> approveSelectedApplication());
        rejectButton.addActionListener(e -> rejectSelectedApplication());

        JPanel allocationPanel = new JPanel(new BorderLayout(8, 8));
        allocationPanel.setBorder(
                BorderFactory.createTitledBorder("Room Allocation")
        );
        allocationPanel.add(bedCombo, BorderLayout.CENTER);

        JPanel actionButtons = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 6)
        );
        actionButtons.add(refreshBedsButton);
        actionButtons.add(approveButton);
        actionButtons.add(rejectButton);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.add(allocationPanel, BorderLayout.NORTH);
        rightPanel.add(detailsScroll, BorderLayout.CENTER);
        rightPanel.add(actionButtons, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScroll,
                rightPanel
        );
        splitPane.setResizeWeight(0.43);
        splitPane.setDividerLocation(290);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(countLabel, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(splitPane, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void loadPendingApplications() {
        tableModel.setRowCount(0);
        applications.clear();
        detailsArea.setText("");
        bedCombo.removeAllItems();

        try {
            applications.addAll(service.getPendingApplications());

            for (PendingApplication app : applications) {
                tableModel.addRow(new Object[]{
                        app.getUserId(),
                        app.getStudentId(),
                        app.getFullName(),
                        app.getEmail(),
                        app.getProfession(),
                        safe(app.getPreferredRoomTypeName()),
                        safe(app.getPreferredRoomNumber()),
                        safe(app.getRegisteredAt())
                });
            }

            countLabel.setText(
                    "Pending applications: " + applications.size()
            );

            if (applications.isEmpty()) {
                detailsArea.setText(
                        "There are no pending student applications."
                );
            }

        } catch (SQLException e) {
            showError(
                    "Unable to load pending applications:\n"
                            + e.getMessage()
            );
        }
    }

    private PendingApplication getSelectedApplication() {
        int selectedRow = applicationTable.getSelectedRow();

        if (selectedRow < 0) {
            return null;
        }

        int modelRow =
                applicationTable.convertRowIndexToModel(selectedRow);

        long userId = Long.parseLong(
                tableModel.getValueAt(modelRow, 0).toString()
        );

        for (PendingApplication app : applications) {
            if (app.getUserId() == userId) {
                return app;
            }
        }

        return null;
    }

    private void showSelectedApplication() {
        PendingApplication app = getSelectedApplication();

        bedCombo.removeAllItems();

        if (app == null) {
            detailsArea.setText("");
            return;
        }

        String institutionOrOrganization;

        if ("STUDENT".equals(app.getProfession())) {
            institutionOrOrganization =
                    "Institute: " + safe(app.getInstitutionName());
        } else {
            institutionOrOrganization =
                    "Organization: " + safe(app.getOrganizationName());
        }

        detailsArea.setText(
                "Full Name: " + safe(app.getFullName()) + "\n"
                + "Email: " + safe(app.getEmail()) + "\n"
                + "Profession: " + safe(app.getProfession()) + "\n"
                + institutionOrOrganization + "\n"
                + "Address: " + safe(app.getAddress()) + "\n"
                + "Phone: " + safe(app.getPhone()) + "\n"
                + "Date of Birth: " + safe(app.getDateOfBirth()) + "\n"
                + "Blood Group: " + safe(app.getBloodGroup()) + "\n"
                + "Guardian: " + safe(app.getGuardianName()) + "\n"
                + "Guardian Phone: " + safe(app.getGuardianPhone()) + "\n"
                + "Citizenship ID: " + safe(app.getCitizenshipId()) + "\n"
                + "National ID: " + safe(app.getNationalId()) + "\n"
                + "Preferred Room Type: "
                + safe(app.getPreferredRoomTypeName()) + "\n"
                + "Preferred Room: "
                + safe(app.getPreferredRoomNumber()) + "\n"
                + "Monthly Fee: " + safe(app.getMonthlyFee()) + "\n"
                + "Registered At: " + safe(app.getRegisteredAt()) + "\n"
                + "Status: PENDING"
        );

        loadAvailableBeds();
    }

    private void loadAvailableBeds() {
        PendingApplication app = getSelectedApplication();

        bedCombo.removeAllItems();

        if (app == null) {
            return;
        }

        try {
            List<AvailableBed> beds =
                    service.getAvailableBeds(app.getStudentId());

            for (AvailableBed bed : beds) {
                bedCombo.addItem(bed);
            }

            if (beds.isEmpty()) {
                detailsArea.append(
                        "\n\nNo compatible available beds found. "
                                + "Check the room type and bed availability."
                );
            }

        } catch (SQLException e) {
            showError(
                    "Unable to load available beds:\n" + e.getMessage()
            );
        }
    }

    private void approveSelectedApplication() {
        PendingApplication app = getSelectedApplication();
        AvailableBed bed = (AvailableBed) bedCombo.getSelectedItem();

        if (app == null) {
            showError("Select a pending application first.");
            return;
        }

        if (bed == null) {
            showError("Select an available bed first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Approve " + app.getFullName() + " and allocate:\n"
                        + bed + "?\n\n"
                        + "The account activates only if allocation succeeds.",
                "Confirm Approval & Allocation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.approveAndAllocate(
                    adminUser.getUserId(),
                    app.getUserId(),
                    app.getStudentId(),
                    bed.getBedId()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Application approved and bed allocated successfully.\n"
                            + "The student's account is now ACTIVE."
            );

            loadPendingApplications();

        } catch (IllegalArgumentException | SQLException e) {
            showError(
                    "Approval/allocation failed:\n" + e.getMessage()
            );
            loadAvailableBeds();
        }
    }

    private void rejectSelectedApplication() {
        PendingApplication app = getSelectedApplication();

        if (app == null) {
            showError("Select a pending application first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Reject the application from " + app.getFullName() + "?\n"
                        + "The account will remain unable to log in.",
                "Confirm Rejection",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.rejectApplication(
                    adminUser.getUserId(),
                    app.getUserId(),
                    app.getStudentId()
            );

            JOptionPane.showMessageDialog(
                    this, "Application rejected."
            );

            loadPendingApplications();

        } catch (SQLException e) {
            showError("Rejection failed:\n" + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Pending Applications",
                JOptionPane.ERROR_MESSAGE
        );
    }
}