package com.hostelmanagement.ui;

import com.hostelmanagement.dao.ComplaintDAO.ComplaintRecord;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.ComplaintService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminComplaintsFrame extends JFrame {
    private final User admin;
    private final ComplaintService service = new ComplaintService();
    private final List<ComplaintRecord> complaints = new ArrayList<>();
    private final JComboBox<String> statusCombo = new JComboBox<>();
    private final JTextArea noteArea = new JTextArea(4, 40);
    private final JTextArea detailsArea = new JTextArea(5, 50);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Student", "Email", "Room", "Category", "Status", "Created"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public AdminComplaintsFrame(User admin) {
        this.admin = admin;
        setTitle("Hostel Management - Maintenance Requests");
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadStatuses();
        refreshComplaints();
    }

    private void buildUI() {
        JLabel title = new JLabel("Room Maintenance Requests", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JButton refresh = new JButton("Refresh Requests");
        refresh.addActionListener(e -> refreshComplaints());
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelected();
        });
        JScrollPane list = new JScrollPane(table);
        list.setBorder(BorderFactory.createTitledBorder("All Complaints"));

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true); detailsArea.setWrapStyleWord(true);
        JScrollPane details = new JScrollPane(detailsArea);
        details.setBorder(BorderFactory.createTitledBorder("Selected Complaint"));
        details.setPreferredSize(new Dimension(700, 120));

        JPanel review = new JPanel(new GridBagLayout());
        review.setBorder(BorderFactory.createTitledBorder("Update Status & Resolution"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7); gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; review.add(new JLabel("New Status:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; review.add(statusCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.BOTH;
        review.add(new JLabel("Resolution / Progress Note:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; review.add(new JScrollPane(noteArea), gbc);
        JButton update = new JButton("Save Complaint Update");
        update.addActionListener(e -> updateComplaint());
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        review.add(update, gbc);

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(details, BorderLayout.NORTH);
        lower.add(review, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, list, lower);
        split.setResizeWeight(0.55); split.setDividerLocation(340);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        setContentPane(content);
    }

    private void loadStatuses() {
        try {
            List<String> statuses = service.getStatuses(admin.getUserId());
            statusCombo.removeAllItems();
            for (String status : statuses) statusCombo.addItem(status);
            if (contains(statuses, "PENDING")) statusCombo.setSelectedItem("PENDING");
        } catch (SQLException e) {
            showError("Could not load complaint statuses:\n" + e.getMessage());
        }
    }

    private boolean contains(List<String> list, String value) {
        for (String item : list) if (item.equalsIgnoreCase(value)) return true;
        return false;
    }

    private void refreshComplaints() {
        try {
            complaints.clear();
            complaints.addAll(service.getAllComplaints(admin.getUserId()));
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ComplaintRecord c : complaints) {
                model.addRow(new Object[]{c.complaintId, c.studentName, c.email,
                        c.roomNumber == null ? "—" : c.roomNumber, c.category, c.status,
                        c.createdAt == null ? "—" : fmt.format(c.createdAt)});
            }
        } catch (SQLException e) { showError("Could not load complaints:\n" + e.getMessage()); }
    }

    private ComplaintRecord selectedRecord() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < 0 || modelRow >= complaints.size() ? null : complaints.get(modelRow);
    }

    private void showSelected() {
        ComplaintRecord c = selectedRecord();
        if (c == null) { detailsArea.setText(""); noteArea.setText(""); return; }
        detailsArea.setText("Complaint #" + c.complaintId
                + "\nStudent: " + c.studentName + " (" + c.email + ")"
                + "\nRoom: " + (c.roomNumber == null ? "—" : c.roomNumber)
                + "\nCategory: " + c.category
                + (c.otherProfessional == null ? "" : " — " + c.otherProfessional)
                + "\nDescription: " + c.description
                + "\nCurrent Status: " + c.status
                + "\nCurrent Note: " + (c.resolutionNote == null ? "—" : c.resolutionNote));
        detailsArea.setCaretPosition(0);
        noteArea.setText(c.resolutionNote == null ? "" : c.resolutionNote);
        statusCombo.setSelectedItem(c.status);
    }

    private void updateComplaint() {
        ComplaintRecord c = selectedRecord();
        if (c == null) { showError("Select a complaint first."); return; }
        String newStatus = (String) statusCombo.getSelectedItem();
        if (newStatus == null) { showError("No status values are configured."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Update complaint #" + c.complaintId + " to " + newStatus + "?",
                "Confirm Complaint Update", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            service.updateComplaint(admin.getUserId(), c.complaintId, newStatus, noteArea.getText().trim());
            JOptionPane.showMessageDialog(this, "Complaint updated successfully.");
            refreshComplaints();
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not update complaint:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Maintenance Requests", JOptionPane.ERROR_MESSAGE);
    }
}
