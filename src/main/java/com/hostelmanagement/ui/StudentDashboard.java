package com.hostelmanagement.ui;

import com.hostelmanagement.dao.StudentDashboardDAO;
import com.hostelmanagement.dao.StudentDashboardDAO.DashboardData;
import com.hostelmanagement.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class StudentDashboard extends JFrame {

    private final User user;

    private final StudentDashboardDAO dao =
            new StudentDashboardDAO();

    private final JLabel welcomeLabel = new JLabel(
            "Welcome", SwingConstants.CENTER
    );

    private final JLabel statusLabel = new JLabel(
            "Account Status: Loading", SwingConstants.CENTER
    );

    private final JTextArea profileArea = createTextArea();
    private final JTextArea roomArea = createTextArea();
    private final JTextArea roommateArea = createTextArea();

    public StudentDashboard(User user) {
        this.user = user;

        setTitle("Hostel Management System - Student Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(800, 540));
        setLocationRelativeTo(null);

        buildUI();
        refreshDashboard();
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 15));
        area.setMargin(new Insets(15, 15, 15, 15));
        return area;
    }

    private void buildUI() {
        JLabel heading = new JLabel(
                "Student Dashboard",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("SansSerif", Font.BOLD, 25));

        welcomeLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 16)
        );

        statusLabel.setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );

        JPanel headingPanel = new JPanel(
                new GridLayout(3, 1, 8, 8)
        );

        headingPanel.add(heading);
        headingPanel.add(welcomeLabel);
        headingPanel.add(statusLabel);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(
                "My Profile",
                new JScrollPane(profileArea)
        );

        tabs.addTab(
                "My Room & Bed",
                new JScrollPane(roomArea)
        );

        tabs.addTab(
                "My Roommates",
                new JScrollPane(roommateArea)
        );

        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.addActionListener(e -> refreshDashboard());

        JButton feesButton = new JButton("Fees & Payments");
        feesButton.addActionListener(e ->
                new StudentFeesFrame(user).setVisible(true)
        );

        JButton foodButton = new JButton("Food Menu & Feedback");
        foodButton.addActionListener(e ->
                new StudentFoodFeedbackFrame(user).setVisible(true)
        );

        JButton complaintsButton = new JButton("Room Complaints & Maintenance");
        complaintsButton.addActionListener(e ->
                new StudentComplaintFrame(user).setVisible(true)
        );

        JButton logoutButton = new JButton("Logout");

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to logout?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 12, 8)
        );

        buttons.add(refreshButton);
        buttons.add(feesButton);
        buttons.add(foodButton);
        buttons.add(complaintsButton);
        buttons.add(logoutButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(
                new JLabel(
                        "For room changes or account issues, contact the Admin.",
                        SwingConstants.CENTER
                ),
                BorderLayout.NORTH
        );

        footer.add(buttons, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(
                new EmptyBorder(20, 25, 20, 25)
        );

        mainPanel.add(headingPanel, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void refreshDashboard() {
        profileArea.setText("Loading student profile...");
        roomArea.setText("Loading room information...");
        roommateArea.setText("Loading roommate information...");

        try {
            DashboardData data =
                    dao.getDashboardData(user.getUserId());

            welcomeLabel.setText(
                    "Welcome, " + safe(data.getFullName())
                            + " (" + data.getEmail() + ")"
            );

            statusLabel.setText(
                    "Account Status: " + safe(data.getAccountStatus())
            );

            showProfile(data);
            showRoom(data);
            showRoommates(data);

        } catch (SQLException e) {
            profileArea.setText(
                    "Could not load your profile.\n\n"
                            + e.getMessage()
            );

            roomArea.setText("Room information is unavailable.");
            roommateArea.setText("Roommate information is unavailable.");

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load Student Dashboard:\n"
                            + e.getMessage(),
                    "Dashboard Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showProfile(DashboardData data) {
        String instituteOrOrganization;

        if ("STUDENT".equalsIgnoreCase(data.getProfession())) {
            instituteOrOrganization =
                    "Institute: " + safe(data.getInstitutionName());
        } else {
            instituteOrOrganization =
                    "Organization: " + safe(data.getOrganizationName());
        }

        profileArea.setText(
                "PERSONAL INFORMATION\n"
                + "----------------------------------------\n"
                + "Student ID: " + data.getStudentId() + "\n"
                + "Full Name: " + safe(data.getFullName()) + "\n"
                + "Email: " + safe(data.getEmail()) + "\n"
                + "Phone: " + safe(data.getPhone()) + "\n"
                + "Address: " + safe(data.getAddress()) + "\n"
                + "Profession: " + safe(data.getProfession()) + "\n"
                + instituteOrOrganization + "\n"
                + "Date of Birth: " + safe(data.getDateOfBirth()) + "\n"
                + "Blood Group: " + safe(data.getBloodGroup()) + "\n\n"

                + "GUARDIAN INFORMATION\n"
                + "----------------------------------------\n"
                + "Guardian Name: " + safe(data.getGuardianName()) + "\n"
                + "Guardian Phone: " + safe(data.getGuardianPhone()) + "\n\n"

                + "ACCOUNT INFORMATION\n"
                + "----------------------------------------\n"
                + "Email: " + safe(data.getEmail()) + "\n"
                + "Status: " + safe(data.getAccountStatus()) + "\n"
                + "Registered At: " + safe(data.getRegisteredAt())
        );

        profileArea.setCaretPosition(0);
    }

    private void showRoom(DashboardData data) {
        if (!data.hasAllocation()) {
            roomArea.setText(
                    "ROOM ALLOCATION\n"
                    + "----------------------------------------\n\n"
                    + "No room or bed has been assigned to your account yet.\n\n"
                    + "Please contact the hostel Admin for assistance.\n\n"
                    + "A room preference submitted during registration "
                    + "does not mean the room has been confirmed."
            );
            return;
        }

        roomArea.setText(
                "MY HOSTEL ACCOMMODATION\n"
                + "----------------------------------------\n"
                + "Hostel: " + safe(data.getHostelName()) + "\n"
                + "Block: " + safe(data.getBlockName()) + "\n"
                + "Floor: " + safe(data.getFloorName()) + "\n"
                + "Room Number: " + safe(data.getRoomNumber()) + "\n"
                + "Bed Number: " + safe(data.getBedNumber()) + "\n\n"

                + "ROOM INFORMATION\n"
                + "----------------------------------------\n"
                + "Room Type: " + safe(data.getRoomTypeName()) + "\n"
                + "Room Capacity: " + data.getCapacity() + " beds\n"
                + "Configured Monthly Room Fee: "
                + safe(data.getMonthlyFee()) + "\n\n"

                + "Your room and bed details are based on your "
                + "current active allocation."
        );

        roomArea.setCaretPosition(0);
    }

    private void showRoommates(DashboardData data) {
        if (!data.hasAllocation()) {
            roommateArea.setText(
                    "Roommate details will appear after "
                            + "a room and bed have been allocated."
            );
            return;
        }

        try {
            List<String> roommates =
                    dao.getRoommates(data.getUserId());

            if (roommates.isEmpty()) {
                roommateArea.setText(
                        "No other student is currently allocated "
                                + "to your room."
                );
                return;
            }

            StringBuilder result = new StringBuilder();

            result.append("YOUR ROOMMATES\n");
            result.append("----------------------------------------\n\n");

            for (int i = 0; i < roommates.size(); i++) {
                result.append(i + 1)
                        .append(". ")
                        .append(roommates.get(i))
                        .append("\n");
            }

            result.append("\nRoommates are based on current active "
                    + "allocations in the same room.");

            roommateArea.setText(result.toString());
            roommateArea.setCaretPosition(0);

        } catch (SQLException e) {
            roommateArea.setText(
                    "Could not load roommates:\n" + e.getMessage()
            );
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "Not provided"
                : value;
    }
}