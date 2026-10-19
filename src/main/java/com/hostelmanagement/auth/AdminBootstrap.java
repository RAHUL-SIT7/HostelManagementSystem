package com.hostelmanagement.auth;

import com.hostelmanagement.dao.UserDAO;

import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;
import java.util.Locale;

public class AdminBootstrap {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminBootstrap::setup);
    }

    private static void setup() {
        UserDAO userDAO = new UserDAO();

        try {
            if (userDAO.countAdmins() > 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "An admin account already exists. Setup is complete."
                );
                return;
            }

            String email = JOptionPane.showInputDialog(
                    null,
                    "Enter the initial admin email:",
                    "Initial Admin Setup",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (email == null) {
                return;
            }

            email = email.trim().toLowerCase(Locale.ROOT);

            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please enter a valid email address."
                );
                return;
            }

            JPasswordField passwordField = new JPasswordField(20);

            int passwordResult = JOptionPane.showConfirmDialog(
                    null,
                    passwordField,
                    "Create admin password (minimum 10 characters)",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (passwordResult != JOptionPane.OK_OPTION) {
                return;
            }

            char[] enteredPassword = passwordField.getPassword();

            if (enteredPassword.length < 10) {
                JOptionPane.showMessageDialog(
                        null,
                        "Password must contain at least 10 characters."
                );
                java.util.Arrays.fill(enteredPassword, '\0');
                return;
            }

            JPasswordField confirmField = new JPasswordField(20);

            int confirmResult = JOptionPane.showConfirmDialog(
                    null,
                    confirmField,
                    "Confirm admin password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (confirmResult != JOptionPane.OK_OPTION) {
                java.util.Arrays.fill(enteredPassword, '\0');
                return;
            }

            char[] confirmedPassword = confirmField.getPassword();

            if (!java.util.Arrays.equals(
                    enteredPassword, confirmedPassword)) {
                JOptionPane.showMessageDialog(
                        null,
                        "Passwords do not match."
                );
                java.util.Arrays.fill(enteredPassword, '\0');
                java.util.Arrays.fill(confirmedPassword, '\0');
                return;
            }

            String password = new String(enteredPassword);
            String passwordHash = PasswordUtil.hashPassword(password);

            userDAO.createAdmin(email, passwordHash);

            java.util.Arrays.fill(enteredPassword, '\0');
            java.util.Arrays.fill(confirmedPassword, '\0');

            JOptionPane.showMessageDialog(
                    null,
                    "Initial admin created successfully!\n"
                            + "You can now open the login screen."
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Database operation failed.\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}