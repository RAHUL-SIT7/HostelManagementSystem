package com.hostelmanagement.ui;

import com.hostelmanagement.model.Room;
import com.hostelmanagement.model.RoomType;
import com.hostelmanagement.service.StudentRegistrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class StudentRegistrationFrame extends JFrame {

    private final StudentRegistrationService service =
            new StudentRegistrationService();

    private final JTextField nameField = new JTextField(22);
    private final JTextField addressField = new JTextField(22);
    private final JTextField phoneField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);

    private final JPasswordField passwordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);

    private final JComboBox<String> professionCombo =
            new JComboBox<>(new String[]{"Student", "Office"});

    private final JTextField institutionField = new JTextField(22);
    private final JTextField organizationField = new JTextField(22);

    private final JTextField dobField =
            new JTextField("YYYY-MM-DD", 22);

    private final JComboBox<String> bloodGroupCombo =
            new JComboBox<>(new String[]{
                    "Select blood group", "A+", "A-", "B+", "B-",
                    "AB+", "AB-", "O+", "O-", "Unknown"
            });

    private final JTextField guardianNameField = new JTextField(22);
    private final JTextField guardianPhoneField = new JTextField(22);
    private final JTextField citizenshipField = new JTextField(22);
    private final JTextField nationalIdField = new JTextField(22);

    private final JComboBox<RoomType> roomTypeCombo = new JComboBox<>();
    private final JLabel feeLabel = new JLabel("Select a room type");
    private final JComboBox<Room> roomCombo = new JComboBox<>();

    private final JPanel professionDetailsPanel =
            new JPanel(new CardLayout());

    private final JCheckBox termsCheckBox = new JCheckBox(
            "I agree to the hostel terms and conditions."
    );

    private final JButton submitButton = new JButton("Submit Registration");

    private boolean loadingRoomTypes = false;

    public StudentRegistrationFrame() {
        setTitle("Hostel Management - Student Registration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(760, 800);
        setMinimumSize(new Dimension(650, 650));
        setLocationRelativeTo(null);

        buildUI();
        registerListeners();
        loadRoomTypes();
        updateProfessionFields();
    }

    private void buildUI() {
        JLabel title = new JLabel(
                "Hostel Registration Form",
                SwingConstants.CENTER
        );

        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel studentPanel = new JPanel(new BorderLayout(8, 0));
        studentPanel.add(new JLabel("Institute Name:"), BorderLayout.WEST);
        studentPanel.add(institutionField, BorderLayout.CENTER);

        JPanel officePanel = new JPanel(new BorderLayout(8, 0));
        officePanel.add(new JLabel("Organization Name:"), BorderLayout.WEST);
        officePanel.add(organizationField, BorderLayout.CENTER);

        professionDetailsPanel.add(studentPanel, "STUDENT");
        professionDetailsPanel.add(officePanel, "OFFICE");

        roomTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus) {

                super.getListCellRendererComponent(
                        list, value, index, selected, focus
                );

                if (value instanceof RoomType type) {
                    setText(type.getTypeName() + " — "
                            + type.getCapacity() + " beds");
                }

                return this;
            }
        });

        roomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus) {

                super.getListCellRendererComponent(
                        list, value, index, selected, focus
                );

                if (value instanceof Room room) {
                    setText("Room " + room.getRoomNumber()
                            + " — " + room.getAvailableBeds()
                            + " beds available");
                }

                return this;
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Applicant Details"));

        int row = 0;

        row = addRow(form, row, "Full Name:", nameField);
        row = addRow(form, row, "Address:", addressField);
        row = addRow(form, row, "Phone Number:", phoneField);
        row = addRow(form, row, "Email:", emailField);
        row = addRow(form, row, "Create Password:", passwordField);
        row = addRow(form, row, "Confirm Password:", confirmPasswordField);
        row = addRow(form, row, "Profession:", professionCombo);
        row = addRow(
                form, row, "Institute / Organization:",
                professionDetailsPanel
        );
        row = addRow(form, row, "Date of Birth (YYYY-MM-DD):", dobField);
        row = addRow(form, row, "Blood Group:", bloodGroupCombo);
        row = addRow(form, row, "Guardian Name:", guardianNameField);
        row = addRow(form, row, "Guardian Phone:", guardianPhoneField);
        row = addRow(form, row, "Citizenship ID:", citizenshipField);
        row = addRow(form, row, "National ID:", nationalIdField);
        row = addRow(form, row, "Preferred Room Type:", roomTypeCombo);
        row = addRow(form, row, "Monthly Fee:", feeLabel);
        addRow(form, row, "Preferred Available Room:", roomCombo);

        submitButton.setEnabled(false);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 8, 8));
        bottom.add(termsCheckBox);
        bottom.add(submitButton);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 20, 15, 20));
        content.add(title, BorderLayout.NORTH);
        content.add(new JScrollPane(form), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private int addRow(
            JPanel panel,
            int row,
            String label,
            Component field
    ) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.insets = new Insets(5, 8, 5, 10);
        left.anchor = GridBagConstraints.WEST;
        left.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.insets = new Insets(5, 8, 5, 8);
        right.fill = GridBagConstraints.HORIZONTAL;

        panel.add(new JLabel(label), left);
        panel.add(field, right);

        return row + 1;
    }

    private void registerListeners() {
        professionCombo.addActionListener(e -> updateProfessionFields());

        roomTypeCombo.addActionListener(e -> {
            if (!loadingRoomTypes) {
                updateRoomTypeSelection();
            }
        });

        termsCheckBox.addActionListener(e ->
                submitButton.setEnabled(termsCheckBox.isSelected())
        );

        submitButton.addActionListener(e -> submitRegistration());
    }

    private void updateProfessionFields() {
        CardLayout cards = (CardLayout) professionDetailsPanel.getLayout();

        if ("Student".equals(professionCombo.getSelectedItem())) {
            cards.show(professionDetailsPanel, "STUDENT");
            organizationField.setText("");
        } else {
            cards.show(professionDetailsPanel, "OFFICE");
            institutionField.setText("");
        }

        professionDetailsPanel.revalidate();
        professionDetailsPanel.repaint();
    }

    private void loadRoomTypes() {
        loadingRoomTypes = true;

        try {
            roomTypeCombo.removeAllItems();

            List<RoomType> roomTypes = service.getActiveRoomTypes();

            for (RoomType type : roomTypes) {
                roomTypeCombo.addItem(type);
            }

        } catch (SQLException e) {
            showError("Unable to load room types:\n" + e.getMessage());
        } finally {
            loadingRoomTypes = false;
        }

        updateRoomTypeSelection();
    }

    private void updateRoomTypeSelection() {
        RoomType selected = (RoomType) roomTypeCombo.getSelectedItem();

        roomCombo.removeAllItems();

        if (selected == null) {
            feeLabel.setText("No active room types available");
            return;
        }

        feeLabel.setText(
                selected.getDefaultMonthlyFee().toPlainString()
        );

        try {
            List<Room> rooms =
                    service.getAvailableRoomsByType(selected.getRoomTypeId());

            for (Room room : rooms) {
                roomCombo.addItem(room);
            }

            if (rooms.isEmpty()) {
                feeLabel.setText(
                        selected.getDefaultMonthlyFee().toPlainString()
                                + " — no rooms currently available"
                );
            }

        } catch (SQLException e) {
            showError("Unable to load available rooms:\n" + e.getMessage());
        }
    }

    private void submitRegistration() {
        if (!termsCheckBox.isSelected()) {
            showError("Please accept the terms and conditions.");
            return;
        }

        char[] passwordChars = passwordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();

        try {
            if (!Arrays.equals(passwordChars, confirmChars)) {
                showError("Passwords do not match.");
                return;
            }

            String password = new String(passwordChars);

            String profession =
                    "Student".equals(professionCombo.getSelectedItem())
                            ? "STUDENT"
                            : "OFFICE";

            String institution = "STUDENT".equals(profession)
                    ? institutionField.getText().trim() : null;

            String organization = "OFFICE".equals(profession)
                    ? organizationField.getText().trim() : null;

            String bloodGroup =
                    (String) bloodGroupCombo.getSelectedItem();

            if (bloodGroup == null
                    || bloodGroup.equals("Select blood group")) {
                showError("Please select a blood group.");
                return;
            }

            LocalDate dateOfBirth;

            try {
                dateOfBirth = LocalDate.parse(dobField.getText().trim());
            } catch (DateTimeParseException e) {
                showError("Enter date of birth as YYYY-MM-DD.");
                return;
            }

            RoomType selectedType =
                    (RoomType) roomTypeCombo.getSelectedItem();

            Room selectedRoom = (Room) roomCombo.getSelectedItem();

            if (selectedType == null || selectedRoom == null) {
                showError("Select a room type and an available room.");
                return;
            }

            service.registerStudent(
                    nameField.getText(),
                    addressField.getText(),
                    phoneField.getText(),
                    emailField.getText(),
                    password,
                    profession,
                    institution,
                    organization,
                    dateOfBirth,
                    bloodGroup,
                    guardianNameField.getText(),
                    guardianPhoneField.getText(),
                    citizenshipField.getText(),
                    nationalIdField.getText(),
                    selectedType.getRoomTypeId(),
                    selectedRoom.getRoomId()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Registration submitted successfully!\n"
                            + "Your account is pending Admin approval.\n"
                            + "Your selected room is a preference, not a confirmed allocation.",
                    "Registration Submitted",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearForm();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            String message = e.getMessage() == null
                    ? "Unknown database error."
                    : e.getMessage();

            if (message.contains("Duplicate entry")) {
                message = "This email may already be registered. "
                        + "Please check your email address.";
            }

            showError("Registration could not be saved:\n" + message);

        } finally {
            Arrays.fill(passwordChars, '\0');
            Arrays.fill(confirmChars, '\0');
            passwordField.setText("");
            confirmPasswordField.setText("");
        }
    }

    private void clearForm() {
        nameField.setText("");
        addressField.setText("");
        phoneField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        institutionField.setText("");
        organizationField.setText("");
        dobField.setText("");
        bloodGroupCombo.setSelectedIndex(0);
        guardianNameField.setText("");
        guardianPhoneField.setText("");
        citizenshipField.setText("");
        nationalIdField.setText("");
        professionCombo.setSelectedIndex(0);
        termsCheckBox.setSelected(false);
        submitButton.setEnabled(false);

        updateProfessionFields();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Student Registration",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new StudentRegistrationFrame().setVisible(true)
        );
    }
}