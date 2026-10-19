package com.hostelmanagement.ui;

import com.hostelmanagement.dao.HostelAvailabilityDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class HostelAvailabilityFrame extends JFrame {

    private final HostelAvailabilityDAO dao =
            new HostelAvailabilityDAO();

    private final DefaultTableModel summaryModel =
            new DefaultTableModel(
                    new Object[]{
                            "Hostel",
                            "Total Rooms",
                            "Total Beds",
                            "Occupied Beds",
                            "Vacant Beds",
                            "Maintenance Beds"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final DefaultTableModel roomModel =
            new DefaultTableModel(
                    new Object[]{
                            "Hostel",
                            "Block",
                            "Floor",
                            "Room",
                            "Room Type",
                            "Capacity",
                            "Beds Created",
                            "Occupied",
                            "Vacant",
                            "Maintenance",
                            "Availability"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable summaryTable = new JTable(summaryModel);
    private final JTable roomTable = new JTable(roomModel);

    private final JLabel totalHostelsLabel = new JLabel("Hostels: 0");
    private final JLabel totalRoomsLabel = new JLabel("Rooms: 0");
    private final JLabel totalBedsLabel = new JLabel("Beds: 0");
    private final JLabel occupiedBedsLabel =
            new JLabel("Occupied: 0");
    private final JLabel vacantBedsLabel =
            new JLabel("Vacant: 0");

    public HostelAvailabilityFrame() {
        setTitle("Hostel Management - Hostel Availability");
        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Hostel Occupancy & Availability",
                SwingConstants.CENTER
        );

        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JButton refreshButton = new JButton("Refresh Availability");
        refreshButton.addActionListener(e -> refreshData());

        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.add(title, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);

        JPanel summaryPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 25, 10)
        );

        summaryPanel.add(totalHostelsLabel);
        summaryPanel.add(totalRoomsLabel);
        summaryPanel.add(totalBedsLabel);
        summaryPanel.add(occupiedBedsLabel);
        summaryPanel.add(vacantBedsLabel);

        summaryTable.setAutoCreateRowSorter(true);
        summaryTable.setRowHeight(26);
        summaryTable.setFillsViewportHeight(true);

        roomTable.setAutoCreateRowSorter(true);
        roomTable.setRowHeight(25);
        roomTable.setFillsViewportHeight(true);

        JScrollPane summaryScroll = new JScrollPane(summaryTable);
        summaryScroll.setBorder(
                BorderFactory.createTitledBorder("Hostel Summary")
        );

        JScrollPane roomScroll = new JScrollPane(roomTable);
        roomScroll.setBorder(
                BorderFactory.createTitledBorder("Room-wise Availability")
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Hostel Summary", summaryScroll);
        tabs.addTab("Room Availability", roomScroll);

        JLabel note = new JLabel(
                "Vacant beds exclude beds already assigned through an active allocation."
        );

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(summaryPanel, BorderLayout.NORTH);
        footer.add(note, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void refreshData() {
        summaryModel.setRowCount(0);
        roomModel.setRowCount(0);

        try {
            List<Object[]> summaries = dao.getHostelSummary();
            List<Object[]> rooms = dao.getRoomAvailability();

            int totalHostels = summaries.size();
            int totalRooms = 0;
            int totalBeds = 0;
            int totalOccupied = 0;
            int totalVacant = 0;

            for (Object[] row : summaries) {
                summaryModel.addRow(row);

                totalRooms += ((Number) row[1]).intValue();
                totalBeds += ((Number) row[2]).intValue();
                totalOccupied += ((Number) row[3]).intValue();
                totalVacant += ((Number) row[4]).intValue();
            }

            for (Object[] row : rooms) {
                roomModel.addRow(row);
            }

            totalHostelsLabel.setText(
                    "Hostels: " + totalHostels
            );

            totalRoomsLabel.setText(
                    "Rooms: " + totalRooms
            );

            totalBedsLabel.setText(
                    "Beds: " + totalBeds
            );

            occupiedBedsLabel.setText(
                    "Occupied: " + totalOccupied
            );

            vacantBedsLabel.setText(
                    "Vacant: " + totalVacant
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load hostel availability:\n" + e.getMessage(),
                    "Availability Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}