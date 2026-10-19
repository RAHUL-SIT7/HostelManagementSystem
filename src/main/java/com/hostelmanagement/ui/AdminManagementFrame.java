package com.hostelmanagement.ui;

import com.hostelmanagement.auth.PasswordUtil;
import com.hostelmanagement.dao.UserDAO;
import com.hostelmanagement.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Admin-only form for creating additional Admin accounts.
 * The DAO rechecks the acting Admin's role/status against MySQL before insert.
 */
public class AdminManagementFrame extends JFrame {

    private final User currentAdmin;
    private final UserDAO userDAO = new UserDAO();

    private final JTextField emailField = new JTextField(25);
    private final JPasswordField passwordField = new JPasswordField(25);
    private final JPasswordField confirmPasswordField = new JPasswordField(25);

    private final JButton createButton = new JButton("Create Admin Account");

    public AdminManagementFrame(User currentAdmin) {
        if (currentAdmin == null
                || !"ADMIN".equalsIgnoreCase(currentAdmin.getRole())) {
            throw new IllegalArgumentException(
                    "Only an Admin can open Admin Management."
            );
        }

        this.currentAdmin = currentAdmin;

        setTitle("Hostel Management - Admin Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 360);
        setMinimumSize(new Dimension(500, 330));
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Create New Admin", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel note = new JLabel(
                "Only an active Admin can create another Admin account.",
                SwingConstants.CENTER
        );

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("New Admin Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addField(form, gbc, 0, "Admin Email:", emailField);
        addField(form, gbc, 1, "Password (10+ characters):", passwordField);
        addField(form, gbc, 2, "Confirm Password:", confirmPasswordField);

        createButton.addActionListener(e -> createAdmin());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearFields());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(clearButton);
        buttons.add(createButton);

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.add(note, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 14));
        content.setBorder(new EmptyBorder(18, 20, 18, 20));
        content.add(title, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(createButton);
    }

    private void addField(
            JPanel panel,
            GridBagConstraints base,
            int row,
            String label,
            JComponent field
    ) {
        GridBagConstraints left = (GridBagConstraints) base.clone();
        left.gridx = 0;
        left.gridy = row;
        left.weightx = 0;
        panel.add(new JLabel(label), left);

        GridBagConstraints right = (GridBagConstraints) base.clone();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        panel.add(field, right);
    }

    private void createAdmin() {
        String email = emailField.getText().trim().toLowerCase(Locale.ROOT);
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmPasswordField.getPassword();

        try {
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                showWarning("Please enter a valid email address.");
                return;
            }

            if (password.length < 10) {
                showWarning("Password must contain at least 10 characters.");
                return;
            }

            if (!Arrays.equals(password, confirmation)) {
                showWarning("Passwords do not match.");
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(new String(password));

            createButton.setEnabled(false);
            try {
                userDAO.createAdminByActiveAdmin(
                        currentAdmin.getUserId(),
                        email,
                        hashedPassword
                );

                JOptionPane.showMessageDialog(
                        this,
                        "New Admin account created successfully.\n"
                                + "The new Admin can now sign in using this email and password.",
                        "Admin Created",
                        JOptionPane.INFORMATION_MESSAGE
                );

                clearFields();

            } catch (SQLException e) {
                String message = e.getMessage() == null
                        ? "Unknown database error."
                        : e.getMessage();

                JOptionPane.showMessageDialog(
                        this,
                        "Could not create Admin account:\n" + message,
                        "Admin Creation Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            } finally {
                createButton.setEnabled(true);
            }

        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            passwordField.setText("");
            confirmPasswordField.setText("");
        }
    }

    private void clearFields() {
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Check Admin Details",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
