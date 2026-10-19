package com.hostelmanagement.ui;

import com.hostelmanagement.model.RoomType;
import com.hostelmanagement.service.RoomTypeService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;

public class RoomTypeManagementFrame extends JFrame {

    private final RoomTypeService service = new RoomTypeService();

    private final JTextField nameField = new JTextField(18);
    private final JTextField capacityField = new JTextField(10);
    private final JTextField feeField = new JTextField(12);

    private long selectedId = 0;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Room Type", "Capacity", "Monthly Fee", "Active"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);

    public RoomTypeManagementFrame() {
        setTitle("Hostel Management - Room Types");
        setSize(850, 570);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadTypes();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Room Type Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        form.setBorder(BorderFactory.createTitledBorder("Room Type Details"));

        form.add(new JLabel("Type Name:"));
        form.add(nameField);

        form.add(new JLabel("Capacity (beds):"));
        form.add(capacityField);

        form.add(new JLabel("Default Monthly Fee:"));
        form.add(feeField);

        JButton addButton = new JButton("Add Room Type");
        JButton updateButton = new JButton("Update Selected");
        JButton deactivateButton = new JButton("Deactivate Selected");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addType());
        updateButton.addActionListener(e -> updateType());
        deactivateButton.addActionListener(e -> deactivateType());
        refreshButton.addActionListener(e -> loadTypes());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectType();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(
                BorderFactory.createTitledBorder("Registered Room Types")
        );

        JPanel top = new JPanel(new BorderLayout(10, 12));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        setContentPane(content);
    }

    private void addType() {
        try {
            int capacity = Integer.parseInt(capacityField.getText().trim());
            BigDecimal fee = new BigDecimal(feeField.getText().trim());

            service.addRoomType(nameField.getText(), capacity, fee);

            JOptionPane.showMessageDialog(this, "Room type added successfully.");
            clearForm();
            loadTypes();

        } catch (NumberFormatException e) {
            showError("Capacity must be a whole number and fee must be numeric.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not add room type. Check for duplicate names.\n"
                    + e.getMessage());
        }
    }

    private void updateType() {
        if (selectedId <= 0) {
            showError("Select a room type from the table first.");
            return;
        }

        try {
            int capacity = Integer.parseInt(capacityField.getText().trim());
            BigDecimal fee = new BigDecimal(feeField.getText().trim());

            service.updateRoomType(
                    selectedId, nameField.getText(), capacity, fee
            );

            JOptionPane.showMessageDialog(this, "Room type updated successfully.");
            clearForm();
            loadTypes();

        } catch (NumberFormatException e) {
            showError("Capacity must be a whole number and fee must be numeric.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update room type.\n" + e.getMessage());
        }
    }

    private void deactivateType() {
        if (selectedId <= 0) {
            showError("Select a room type first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Deactivate the selected room type?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deactivateRoomType(selectedId);

            JOptionPane.showMessageDialog(
                    this, "Room type deactivated successfully."
            );

            clearForm();
            loadTypes();

        } catch (SQLException e) {
            showError("Could not deactivate room type.\n" + e.getMessage());
        }
    }

    private void loadTypes() {
        tableModel.setRowCount(0);

        try {
            for (RoomType type : service.getAllRoomTypes()) {
                tableModel.addRow(new Object[]{
                        type.getRoomTypeId(),
                        type.getTypeName(),
                        type.getCapacity(),
                        type.getDefaultMonthlyFee(),
                        type.isActive()
                });
            }
        } catch (SQLException e) {
            showError("Could not load room types.\n" + e.getMessage());
        }
    }

    private void selectType() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int row = table.convertRowIndexToModel(selectedRow);

        selectedId = Long.parseLong(
                tableModel.getValueAt(row, 0).toString()
        );

        nameField.setText(tableModel.getValueAt(row, 1).toString());
        capacityField.setText(tableModel.getValueAt(row, 2).toString());
        feeField.setText(tableModel.getValueAt(row, 3).toString());
    }

    private void clearForm() {
        selectedId = 0;
        nameField.setText("");
        capacityField.setText("");
        feeField.setText("");
        table.clearSelection();
        nameField.requestFocusInWindow();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Room Type Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}