package com.hostelmanagement.ui;

import com.hostelmanagement.model.Hostel;
import com.hostelmanagement.service.HostelService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class HostelManagementFrame extends JFrame {

    private final HostelService hostelService = new HostelService();

    private final JTextField nameField = new JTextField(20);
    private final JTextField codeField = new JTextField(12);
    private final JTextField addressField = new JTextField(25);

    private final JComboBox<String> typeCombo = new JComboBox<>(
            new String[]{"BOYS", "GIRLS", "MIXED", "STAFF", "OTHER"}
    );

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Hostel Name", "Code",
                    "Type", "Address", "Active"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable hostelTable = new JTable(tableModel);
    private long selectedHostelId = 0;

    public HostelManagementFrame() {
        setTitle("Hostel Management - Infrastructure");
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadHostels();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Hostel Infrastructure Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setBorder(new EmptyBorder(15, 10, 15, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Hostel Details"));

        form.add(new JLabel("Hostel Name:"));
        form.add(nameField);

        form.add(new JLabel("Hostel Code:"));
        form.add(codeField);

        form.add(new JLabel("Hostel Type:"));
        form.add(typeCombo);

        form.add(new JLabel("Address:"));
        form.add(addressField);

        JButton addButton = new JButton("Add Hostel");
        JButton updateButton = new JButton("Update Selected");
        JButton deactivateButton = new JButton("Deactivate Selected");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addHostel());
        updateButton.addActionListener(e -> updateHostel());
        deactivateButton.addActionListener(e -> deactivateHostel());
        refreshButton.addActionListener(e -> loadHostels());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);

        hostelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hostelTable.setAutoCreateRowSorter(true);
        hostelTable.setRowHeight(25);
        hostelTable.getColumnModel().getColumn(0).setPreferredWidth(45);

        hostelTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectHostelFromTable();
            }
        });

        JScrollPane tableScroll = new JScrollPane(hostelTable);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder("Registered Hostels")
        );

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);

        setContentPane(content);
    }

    private void addHostel() {
        try {
            hostelService.addHostel(
                    nameField.getText(),
                    codeField.getText(),
                    addressField.getText(),
                    (String) typeCombo.getSelectedItem()
            );

            showMessage("Hostel added successfully.");
            clearForm();
            loadHostels();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private void updateHostel() {
        if (selectedHostelId == 0) {
            showError("Select a hostel from the table first.");
            return;
        }

        try {
            hostelService.updateHostel(
                    selectedHostelId,
                    nameField.getText(),
                    codeField.getText(),
                    addressField.getText(),
                    (String) typeCombo.getSelectedItem()
            );

            showMessage("Hostel updated successfully.");
            clearForm();
            loadHostels();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private void deactivateHostel() {
        if (selectedHostelId == 0) {
            showError("Select a hostel from the table first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Deactivate the selected hostel?",
                "Confirm Deactivation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            hostelService.deactivateHostel(selectedHostelId);

            showMessage("Hostel deactivated. Its record was preserved.");
            clearForm();
            loadHostels();

        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private void loadHostels() {
        try {
            tableModel.setRowCount(0);

            for (Hostel hostel : hostelService.getAllHostels()) {
                tableModel.addRow(new Object[]{
                        hostel.getHostelId(),
                        hostel.getHostelName(),
                        hostel.getHostelCode(),
                        hostel.getHostelType(),
                        hostel.getAddress(),
                        hostel.isActive()
                });
            }

        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private void selectHostelFromTable() {
        int selectedRow = hostelTable.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int modelRow = hostelTable.convertRowIndexToModel(selectedRow);

        selectedHostelId = Long.parseLong(
                tableModel.getValueAt(modelRow, 0).toString()
        );

        nameField.setText(tableModel.getValueAt(modelRow, 1).toString());
        codeField.setText(tableModel.getValueAt(modelRow, 2).toString());
        typeCombo.setSelectedItem(tableModel.getValueAt(modelRow, 3));

        Object address = tableModel.getValueAt(modelRow, 4);
        addressField.setText(address == null ? "" : address.toString());
    }

    private void clearForm() {
        selectedHostelId = 0;
        nameField.setText("");
        codeField.setText("");
        addressField.setText("");
        typeCombo.setSelectedIndex(0);
        hostelTable.clearSelection();
        nameField.requestFocusInWindow();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Hostel Management",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Validation",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showDatabaseError(SQLException e) {
        String message = e.getMessage() == null
                ? "Unknown database error."
                : e.getMessage();

        JOptionPane.showMessageDialog(
                this,
                "Database operation failed:\n" + message,
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );

        System.err.println("Hostel DAO error: " + message);
    }
}