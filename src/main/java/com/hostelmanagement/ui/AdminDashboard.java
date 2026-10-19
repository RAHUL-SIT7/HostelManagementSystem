package com.hostelmanagement.ui;

import com.hostelmanagement.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminDashboard extends JFrame {
    private final User user;

    public AdminDashboard(User user) {
        this.user = user;
        setTitle("Hostel Management System - Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(930, 850);
        setMinimumSize(new Dimension(700, 650));
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Hostel Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Admin Dashboard | Welcome, " + user.getEmail(), SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton hostelButton = new JButton("Hostel Management");
        JButton blockButton = new JButton("Block Management");
        JButton floorButton = new JButton("Floor Management");
        JButton roomTypeButton = new JButton("Room Type Management");
        JButton roomButton = new JButton("Room Management");
        JButton availabilityButton = new JButton("Hostel Availability & Occupancy");
        JButton applicationsButton = new JButton("Pending Student Applications");
        JButton paymentsButton = new JButton("Payment Verification");
        JButton foodMenuButton = new JButton("Food Menu & Meal Timings");
        JButton foodAnalyticsButton = new JButton("Food Feedback & Analytics");
        JButton complaintsButton = new JButton("Room Complaints & Maintenance");
        JButton logoutButton = new JButton("Logout");

        hostelButton.addActionListener(e -> new HostelManagementFrame().setVisible(true));
        blockButton.addActionListener(e -> new BlockManagementFrame().setVisible(true));
        floorButton.addActionListener(e -> new FloorManagementFrame().setVisible(true));
        roomTypeButton.addActionListener(e -> new RoomTypeManagementFrame().setVisible(true));
        roomButton.addActionListener(e -> new RoomManagementFrame().setVisible(true));
        availabilityButton.addActionListener(e -> new HostelAvailabilityFrame().setVisible(true));
        applicationsButton.addActionListener(e -> new PendingApplicationsFrame(user).setVisible(true));
        paymentsButton.addActionListener(e -> new AdminPaymentsFrame(user).setVisible(true));
        foodMenuButton.addActionListener(e -> new AdminFoodMenuFrame(user).setVisible(true));
        foodAnalyticsButton.addActionListener(e -> new AdminFoodAnalyticsFrame(user).setVisible(true));
        complaintsButton.addActionListener(e -> new AdminComplaintsFrame(user).setVisible(true));

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        JPanel heading = new JPanel(new GridLayout(2, 1, 8, 8));
        heading.add(title);
        heading.add(subtitle);

        JPanel modules = new JPanel(new GridLayout(0, 1, 10, 10));
        modules.setBorder(new EmptyBorder(12, 8, 12, 8));
        modules.add(hostelButton);
        modules.add(blockButton);
        modules.add(floorButton);
        modules.add(roomTypeButton);
        modules.add(roomButton);
        modules.add(availabilityButton);
        modules.add(applicationsButton);
        modules.add(paymentsButton);
        modules.add(foodMenuButton);
        modules.add(foodAnalyticsButton);
        modules.add(complaintsButton);
        modules.add(logoutButton);

        JScrollPane moduleScroll = new JScrollPane(modules);
        moduleScroll.setBorder(BorderFactory.createEmptyBorder());
        moduleScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setBorder(new EmptyBorder(22, 35, 22, 35));
        content.add(heading, BorderLayout.NORTH);
        content.add(moduleScroll, BorderLayout.CENTER);
        setContentPane(content);
    }
}
