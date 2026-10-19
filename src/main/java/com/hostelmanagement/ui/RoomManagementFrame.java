package com.hostelmanagement.ui;

import com.hostelmanagement.dao.BlockDAO;
import com.hostelmanagement.dao.FloorDAO;
import com.hostelmanagement.dao.RoomTypeDAO;
import com.hostelmanagement.model.Block;
import com.hostelmanagement.model.Floor;
import com.hostelmanagement.model.Hostel;
import com.hostelmanagement.model.Room;
import com.hostelmanagement.model.RoomType;
import com.hostelmanagement.service.HostelService;
import com.hostelmanagement.service.RoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class RoomManagementFrame extends JFrame {

    private final HostelService hostelService = new HostelService();
    private final BlockDAO blockDAO = new BlockDAO();
    private final FloorDAO floorDAO = new FloorDAO();
    private final RoomTypeDAO roomTypeDAO = new RoomTypeDAO();
    private final RoomService roomService = new RoomService();

    private final JComboBox<Hostel> hostelCombo = new JComboBox<>();
    private final JComboBox<Block> blockCombo = new JComboBox<>();
    private final JComboBox<Floor> floorCombo = new JComboBox<>();
    private final JComboBox<RoomType> roomTypeCombo = new JComboBox<>();

    private final JTextField roomNumberField = new JTextField(15);

    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"ACTIVE", "MAINTENANCE", "INACTIVE"}
    );

    private boolean loadingBlocks;
    private boolean loadingFloors;
    private long selectedRoomId;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "Room ID", "Room Number", "Room Type",
                    "Capacity", "Total Beds", "Available Beds", "Status"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable roomTable = new JTable(tableModel);

    public RoomManagementFrame() {
        setTitle("Hostel Management - Room Management");
        setSize(1100, 650);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();

        loadHostels();
        loadRoomTypes();

        registerComboListeners();
        loadBlocks();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Room Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        blockCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

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

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Room Details"));

        form.add(new JLabel("Hostel:"));
        form.add(hostelCombo);

        form.add(new JLabel("Block:"));
        form.add(blockCombo);

        form.add(new JLabel("Floor:"));
        form.add(floorCombo);

        form.add(new JLabel("Room Type:"));
        form.add(roomTypeCombo);

        form.add(new JLabel("Room Number:"));
        form.add(roomNumberField);

        form.add(new JLabel("Room Status:"));
        form.add(statusCombo);

        JButton addButton = new JButton("Add Room + Create Beds");
        JButton updateButton = new JButton("Update Selected");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addRoom());
        updateButton.addActionListener(e -> updateRoom());
        refreshButton.addActionListener(e -> loadRooms());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);

        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setAutoCreateRowSorter(true);
        roomTable.setRowHeight(25);

        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectRoom();
            }
        });

        JScrollPane tableScroll = new JScrollPane(roomTable);
        tableScroll.setBorder(
                BorderFactory.createTitledBorder("Rooms in Selected Floor")
        );

        JPanel top = new JPanel(new BorderLayout(10, 12));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(top, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);

        setContentPane(content);
    }

    private void registerComboListeners() {
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

        floorCombo.addActionListener(e -> {
            if (!loadingFloors) {
                clearForm();
                loadRooms();
            }
        });
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
                for (Block block : blockDAO.getBlocksByHostel(
                        hostel.getHostelId())) {
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
        loadingFloors = true;

        try {
            floorCombo.removeAllItems();

            Block block = (Block) blockCombo.getSelectedItem();

            if (block != null) {
                for (Floor floor : floorDAO.getFloorsByBlock(
                        block.getBlockId())) {
                    floorCombo.addItem(floor);
                }
            }
        } catch (SQLException e) {
            showError("Unable to load floors: " + e.getMessage());
        } finally {
            loadingFloors = false;
        }

        loadRooms();
    }

    private void loadRoomTypes() {
        try {
            for (RoomType type : roomTypeDAO.getAllRoomTypes()) {
                if (type.isActive()) {
                    roomTypeCombo.addItem(type);
                }
            }
        } catch (SQLException e) {
            showError("Unable to load room types: " + e.getMessage());
        }
    }

    private void loadRooms() {
        tableModel.setRowCount(0);

        Floor floor = (Floor) floorCombo.getSelectedItem();

        if (floor == null) {
            return;
        }

        try {
            for (Room room : roomService.getRoomsByFloor(
                    floor.getFloorId())) {

                tableModel.addRow(new Object[]{
                        room.getRoomId(),
                        room.getRoomNumber(),
                        room.getRoomTypeName(),
                        room.getCapacity(),
                        room.getTotalBeds(),
                        room.getAvailableBeds(),
                        room.getRoomStatus()
                });
            }
        } catch (SQLException e) {
            showError("Unable to load rooms: " + e.getMessage());
        }
    }

    private void addRoom() {
        Floor floor = (Floor) floorCombo.getSelectedItem();
        RoomType roomType = (RoomType) roomTypeCombo.getSelectedItem();

        if (floor == null) {
            showError("Select a hostel, block and floor first.");
            return;
        }

        if (roomType == null) {
            showError("Create and select an active room type first.");
            return;
        }

        try {
            int capacity = roomService.addRoom(
                    floor.getFloorId(),
                    roomType.getRoomTypeId(),
                    roomNumberField.getText()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Room created successfully.\n"
                            + capacity + " beds were created automatically."
            );

            clearForm();
            loadRooms();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError(
                    "Room could not be created. Check for a duplicate room "
                            + "number or inactive room type.\n"
                            + e.getMessage()
            );
        }
    }

    private void updateRoom() {
        if (selectedRoomId <= 0) {
            showError("Select a room from the table first.");
            return;
        }

        try {
            roomService.updateRoom(
                    selectedRoomId,
                    roomNumberField.getText(),
                    (String) statusCombo.getSelectedItem()
            );

            JOptionPane.showMessageDialog(
                    this, "Room updated successfully."
            );

            clearForm();
            loadRooms();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Unable to update room.\n" + e.getMessage());
        }
    }

    private void selectRoom() {
        int selectedRow = roomTable.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int row = roomTable.convertRowIndexToModel(selectedRow);

        selectedRoomId = Long.parseLong(
                tableModel.getValueAt(row, 0).toString()
        );

        roomNumberField.setText(
                tableModel.getValueAt(row, 1).toString()
        );

        statusCombo.setSelectedItem(
                tableModel.getValueAt(row, 6).toString()
        );
    }

    private void clearForm() {
        selectedRoomId = 0;
        roomNumberField.setText("");
        statusCombo.setSelectedItem("ACTIVE");
        roomTable.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Room Management",
                JOptionPane.ERROR_MESSAGE
        );
    }
}