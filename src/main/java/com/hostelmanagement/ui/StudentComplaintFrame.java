package com.hostelmanagement.ui;

import com.hostelmanagement.dao.ComplaintDAO.ComplaintRecord;
import com.hostelmanagement.dao.ComplaintDAO.RoomInfo;
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

public class StudentComplaintFrame extends JFrame {
    private final User student;
    private final ComplaintService service = new ComplaintService();
    private final JLabel roomLabel = new JLabel("Loading room allocation...");
    private final JComboBox<String> categoryCombo = new JComboBox<>();
    private final JTextField otherProfessionalField = new JTextField(25);
    private final JTextArea descriptionArea = new JTextArea(5, 35);
    private final List<ComplaintRecord> complaints = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Category", "Room", "Status", "Submitted", "Resolution Note"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextArea selectedDetails = new JTextArea(4, 45);

    public StudentComplaintFrame(User student) {
        this.student = student;
        setTitle("Hostel Management - Maintenance Complaints");
        setSize(1050, 720);
        setMinimumSize(new Dimension(800, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadCategoriesAndRoom();
        refreshComplaints();
    }

    private void buildUI() {
        JLabel title = new JLabel("Room Maintenance & Complaints", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JPanel heading = new JPanel(new BorderLayout(6, 6));
        heading.add(title, BorderLayout.NORTH);
        roomLabel.setBorder(new EmptyBorder(5, 8, 5, 8));
        heading.add(roomLabel, BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Report a Room Problem"));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        GridBagConstraints gbc = constraints();
        row(form, gbc, 0, "Problem Type:", categoryCombo);
        row(form, gbc, 1, "Other Professional (optional):", otherProfessionalField);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        form.add(new JLabel("Describe the Problem:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(new JScrollPane(descriptionArea), gbc);
        JButton submit = new JButton("Submit Complaint");
        JButton refresh = new JButton("Refresh My Complaints");
        submit.addActionListener(e -> submitComplaint());
        refresh.addActionListener(e -> refreshComplaints());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh); actions.add(submit);
        JPanel formWrap = new JPanel(new BorderLayout(8, 8));
        formWrap.add(form, BorderLayout.CENTER); formWrap.add(actions, BorderLayout.SOUTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedComplaint();
        });
        JScrollPane history = new JScrollPane(table);
        history.setBorder(BorderFactory.createTitledBorder("My Complaint History"));
        selectedDetails.setEditable(false);
        selectedDetails.setLineWrap(true);
        selectedDetails.setWrapStyleWord(true);
        JScrollPane details = new JScrollPane(selectedDetails);
        details.setBorder(BorderFactory.createTitledBorder("Selected Complaint Details"));
        details.setPreferredSize(new Dimension(700, 100));

        JSplitPane bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, history, details);
        bottom.setResizeWeight(0.72);
        bottom.setDividerLocation(260);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formWrap, bottom);
        main.setResizeWeight(0.46);
        main.setDividerLocation(300);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        content.add(heading, BorderLayout.NORTH);
        content.add(main, BorderLayout.CENTER);
        setContentPane(content);
    }

    private GridBagConstraints constraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void row(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0; gbc.weighty = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void loadCategoriesAndRoom() {
        try {
            RoomInfo room = service.getCurrentRoom(student.getUserId());
            roomLabel.setText("Current allocation: " + room);
            List<String> categories = service.getCategories(student.getUserId());
            categoryCombo.removeAllItems();
            for (String category : categories) categoryCombo.addItem(category);
        } catch (SQLException e) {
            roomLabel.setText("Room allocation could not be loaded.");
            showError(e.getMessage());
        }
    }

    private void submitComplaint() {
        String category = (String) categoryCombo.getSelectedItem();
        String description = descriptionArea.getText().trim();
        if (category == null) { showError("No complaint category is configured in the database."); return; }
        if (description.isBlank()) { showError("Describe the issue before submitting."); return; }
        try {
            long id = service.submitComplaint(student.getUserId(), category,
                    otherProfessionalField.getText().trim(), description);
            JOptionPane.showMessageDialog(this,
                    "Complaint submitted successfully.\nComplaint ID: " + id + "\nCurrent status: PENDING");
            descriptionArea.setText("");
            otherProfessionalField.setText("");
            refreshComplaints();
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not submit complaint:\n" + e.getMessage());
        }
    }

    private void refreshComplaints() {
        try {
            complaints.clear();
            complaints.addAll(service.getStudentComplaints(student.getUserId()));
            model.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ComplaintRecord c : complaints) {
                model.addRow(new Object[]{c.complaintId, c.category,
                        c.roomNumber == null ? "—" : c.roomNumber, c.status,
                        c.createdAt == null ? "—" : formatter.format(c.createdAt),
                        c.resolutionNote == null ? "" : c.resolutionNote});
            }
        } catch (SQLException e) {
            showError("Could not load complaint history:\n" + e.getMessage());
        }
    }

    private void showSelectedComplaint() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= complaints.size()) { selectedDetails.setText(""); return; }
        ComplaintRecord c = complaints.get(row);
        selectedDetails.setText("Complaint #" + c.complaintId + " | " + c.category
                + (c.otherProfessional == null ? "" : " — " + c.otherProfessional)
                + "\nDescription: " + c.description
                + "\nStatus: " + c.status
                + "\nResolution: " + (c.resolutionNote == null ? "Not updated yet" : c.resolutionNote));
        selectedDetails.setCaretPosition(0);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Maintenance Complaints", JOptionPane.ERROR_MESSAGE);
    }
}
