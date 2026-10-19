package com.hostelmanagement.auth;

import com.hostelmanagement.dao.UserDAO;
import com.hostelmanagement.model.User;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public User login(String email, String password)
            throws SQLException {

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter your email and password."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<UserDAO.CredentialRecord> record =
                userDAO.findCredentialsByEmail(normalizedEmail);

        if (record.isEmpty()
                || !PasswordUtil.verifyPassword(
                        password,
                        record.map(UserDAO.CredentialRecord::getPasswordHash)
                                .orElse(null)
                )) {
            throw new IllegalArgumentException(
                    "Invalid email or password."
            );
        }

        User user = record.get().getUser();

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new IllegalStateException(
                    "Your account is not active. Please wait for admin approval."
            );
        }

        return user;
    }
}