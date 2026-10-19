package com.hostelmanagement.ui;

import com.hostelmanagement.dao.BlockDAO;
import com.hostelmanagement.model.Block;
import com.hostelmanagement.model.Floor;
import com.hostelmanagement.model.Hostel;
import com.hostelmanagement.service.FloorService;
import com.hostelmanagement.service.HostelService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class FloorManagementFrame extends JFrame {

    private final HostelService hostelService = new HostelService();
    private final BlockDAO blockDAO = new BlockDAO();
    private final FloorService floorService = new FloorService();

    private final JComboBox<Hostel> hostelCombo = new JComboBox<>();
    private final JComboBox<Block> blockCombo = new JComboBox<>();
    private final JTextField numberField = new JTextField(10);
    private final JTextField nameField = new JTextField(18);

    private boolean loadingBlocks = false;
    private long selectedFloorId = 0;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Floor ID", "Floor Number", "Floor Name"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable floorTable = new JTable(tableModel);

    public FloorManagementFrame() {
        setTitle("Hostel Management - Floor Management");
        setSize(900, 570);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadHostels();

        hostelCombo.addActionListener(e -> {
            clearForm();
            loadBlocks();
        });

        blockCombo.addActionListener(e -> {
            if (!loadingBlocks) {
                clearForm();
                loadFloors();
            }
        });

        loadBlocks();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Floor Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        blockCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof Block block) {
                    setText(block.getBlockName()
                            + " (" + block.getBlockCode() + ")");
                }

                return this;
            }
        });

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));
        form.setBorder(BorderFactory.createTitledBorder("Floor Details"));

        form.add(new JLabel("Select Hostel:"));
        form.add(hostelCombo);

        form.add(new JLabel("Select Block:"));
        form.add(blockCombo);

        form.add(new JLabel("Floor Number (0 = Ground):"));
        form.add(numberField);

        form.add(new JLabel("Floor Name:"));
        form.add(nameField);

        JButton addButton = new JButton("Add Floor");
        JButton updateButton = new JButton("Update Selected");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addFloor());
        updateButton.addActionListener(e -> updateFloor());
        refreshButton.addActionListener(e -> loadFloors());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);

        floorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        floorTable.setAutoCreateRowSorter(true);
        floorTable.setRowHeight(25);

        floorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectFloor();
            }
        });

        JScrollPane scrollPane = new JScrollPane(floorTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Floors in Selected Block")
        );

        JPanel top = new JPanel(new BorderLayout(10, 15));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);

        setContentPane(content);
    }

    private void loadHostels() {
        try {
            for (Hostel hostel : hostelService.getAllHostels()) {
                if (hostel.isActive()) {
                    hostelCombo.addItem(hostel);
                }
            }
        } catch (SQLException e) {
            showError("Unable to load hostels: " + e.getMessage());
        }
    }

    private void loadBlocks() {
        loadingBlocks = true;

        try {
            blockCombo.removeAllItems();

            Hostel hostel = (Hostel) hostelCombo.getSelectedItem();

            if (hostel != null) {
                for (Block block :
                        blockDAO.getBlocksByHostel(hostel.getHostelId())) {
                    blockCombo.addItem(block);
                }
            }
        } catch (SQLException e) {
            showError("Unable to load blocks: " + e.getMessage());
        } finally {
            loadingBlocks = false;
        }

        loadFloors();
    }

    private void loadFloors() {
        tableModel.setRowCount(0);

        Block block = (Block) blockCombo.getSelectedItem();

        if (block == null) {
            return;
        }

        try {
            for (Floor floor :
                    floorService.getFloorsByBlock(block.getBlockId())) {
                tableModel.addRow(new Object[]{
                        floor.getFloorId(),
                        floor.getFloorNumber(),
                        floor.getFloorName()
                });
            }
        } catch (SQLException e) {
            showError("Unable to load floors: " + e.getMessage());
        }
    }

    private void addFloor() {
        Block block = (Block) blockCombo.getSelectedItem();

        if (block == null) {
            showError("Select a hostel and block first.");
            return;
        }

        try {
            int floorNumber = Integer.parseInt(numberField.getText().trim());

            floorService.addFloor(
                    block.getBlockId(),
                    floorNumber,
                    nameField.getText()
            );

            JOptionPane.showMessageDialog(this, "Floor added successfully.");

            clearForm();
            loadFloors();

        } catch (NumberFormatException e) {
            showError("Floor number must be a whole number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Unable to add floor. Check for duplicate floor numbers.\n"
                    + e.getMessage());
        }
    }

    private void updateFloor() {
        if (selectedFloorId <= 0) {
            showError("Select a floor from the table first.");
            return;
        }

        try {
            int floorNumber = Integer.parseInt(numberField.getText().trim());

            floorService.updateFloor(
                    selectedFloorId,
                    floorNumber,
                    nameField.getText()
            );

            JOptionPane.showMessageDialog(
                    this, "Floor updated successfully."
            );

            clearForm();
            loadFloors();

        } catch (NumberFormatException e) {
            showError("Floor number must be a whole number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Unable to update floor. Check for duplicate numbers.\n"
                    + e.getMessage());
        }
    }

    private void selectFloor() {
        int selectedRow = floorTable.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int row = floorTable.convertRowIndexToModel(selectedRow);

        selectedFloorId = Long.parseLong(
                tableModel.getValueAt(row, 0).toString()
        );

        numberField.setText(tableModel.getValueAt(row, 1).toString());
        nameField.setText(tableModel.getValueAt(row, 2).toString());
    }

    private void clearForm() {
        selectedFloorId = 0;
        numberField.setText("");
        nameField.setText("");
        floorTable.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Floor Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}