package com.hostelmanagement.ui;

import com.hostelmanagement.model.Block;
import com.hostelmanagement.model.Hostel;
import com.hostelmanagement.service.BlockService;
import com.hostelmanagement.service.HostelService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class BlockManagementFrame extends JFrame {

    private final HostelService hostelService = new HostelService();
    private final BlockService blockService = new BlockService();

    private final JComboBox<Hostel> hostelCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField(18);
    private final JTextField codeField = new JTextField(12);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Block ID", "Block Name", "Block Code"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable blockTable = new JTable(tableModel);
    private long selectedBlockId = 0;

    public BlockManagementFrame() {
        setTitle("Hostel Management - Block Management");
        setSize(850, 550);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadHostels();

        hostelCombo.addActionListener(e -> {
            clearForm();
            loadBlocks();
        });
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Block Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        form.setBorder(BorderFactory.createTitledBorder("Block Details"));

        form.add(new JLabel("Select Hostel:"));
        form.add(hostelCombo);

        form.add(new JLabel("Block Name:"));
        form.add(nameField);

        form.add(new JLabel("Block Code:"));
        form.add(codeField);

        JButton addButton = new JButton("Add Block");
        JButton updateButton = new JButton("Update Selected");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addBlock());
        updateButton.addActionListener(e -> updateBlock());
        refreshButton.addActionListener(e -> loadBlocks());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);

        blockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        blockTable.setAutoCreateRowSorter(true);
        blockTable.setRowHeight(25);

        blockTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectBlock();
            }
        });

        JScrollPane tableScroll = new JScrollPane(blockTable);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder("Blocks in Selected Hostel")
        );

        JPanel top = new JPanel(new BorderLayout(10, 15));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);

        setContentPane(content);
    }

    private void loadHostels() {
        try {
            hostelCombo.removeAllItems();

            for (Hostel hostel : hostelService.getAllHostels()) {
                if (hostel.isActive()) {
                    hostelCombo.addItem(hostel);
                }
            }

            loadBlocks();

        } catch (SQLException e) {
            showError("Unable to load hostels: " + e.getMessage());
        }
    }

    private void loadBlocks() {
        tableModel.setRowCount(0);

        Hostel hostel = (Hostel) hostelCombo.getSelectedItem();

        if (hostel == null) {
            return;
        }

        try {
            for (Block block :
                    blockService.getBlocksByHostel(hostel.getHostelId())) {

                tableModel.addRow(new Object[]{
                        block.getBlockId(),
                        block.getBlockName(),
                        block.getBlockCode()
                });
            }
        } catch (SQLException e) {
            showError("Unable to load blocks: " + e.getMessage());
        }
    }

    private void addBlock() {
        Hostel hostel = (Hostel) hostelCombo.getSelectedItem();

        if (hostel == null) {
            showError("Create or select an active hostel first.");
            return;
        }

        try {
            blockService.addBlock(
                    hostel.getHostelId(),
                    nameField.getText(),
                    codeField.getText()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Block added successfully."
            );

            clearForm();
            loadBlocks();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Unable to add block: " + e.getMessage());
        }
    }

    private void updateBlock() {
        if (selectedBlockId <= 0) {
            showError("Select a block from the table first.");
            return;
        }

        try {
            blockService.updateBlock(
                    selectedBlockId,
                    nameField.getText(),
                    codeField.getText()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Block updated successfully."
            );

            clearForm();
            loadBlocks();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Unable to update block: " + e.getMessage());
        }
    }

    private void selectBlock() {
        int selectedRow = blockTable.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int row = blockTable.convertRowIndexToModel(selectedRow);

        selectedBlockId = Long.parseLong(
                tableModel.getValueAt(row, 0).toString()
        );

        nameField.setText(tableModel.getValueAt(row, 1).toString());
        codeField.setText(tableModel.getValueAt(row, 2).toString());
    }

    private void clearForm() {
        selectedBlockId = 0;
        nameField.setText("");
        codeField.setText("");
        blockTable.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Block Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}