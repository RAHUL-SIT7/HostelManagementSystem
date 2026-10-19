package com.hostelmanagement.ui;

import com.hostelmanagement.auth.AuthService;
import com.hostelmanagement.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("Hostel Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(430, 340);
        setMinimumSize(new Dimension(430, 340));
        setLocationRelativeTo(null);
        setResizable(false);

        JLabel title = new JLabel(
                "Hostel Management System",
                SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 19));

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 12));
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> performLogin());

        JButton registerButton = new JButton("New User? Register Here");
        registerButton.addActionListener(e -> openStudentRegistration());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        JPanel content = new JPanel(new BorderLayout(12, 20));
        content.setBorder(new EmptyBorder(25, 25, 25, 25));
        content.add(title, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(loginButton);
    }

    private void openStudentRegistration() {
        // Keep the login window open so the student can return after registering.
        StudentRegistrationFrame registrationFrame = new StudentRegistrationFrame();
        registrationFrame.setLocationRelativeTo(this);
        registrationFrame.setVisible(true);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);

        try {
            User user = authService.login(email, password);

            dispose();

            if ("ADMIN".equals(user.getRole())) {
                new AdminDashboard(user).setVisible(true);
            } else if ("STUDENT".equals(user.getRole())) {
                new StudentDashboard(user).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Unknown account role."
                );
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Login Failed",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to access the database. Check the connection settings.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            System.err.println("Login database error: " + e.getMessage());

        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
            passwordField.setText("");
        }
    }
}
