package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;
import com.hostelmanagement.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class UserDAO {

    public static final class CredentialRecord {

        private final User user;
        private final String passwordHash;

        public CredentialRecord(User user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }

        public User getUser() {
            return user;
        }

        public String getPasswordHash() {
            return passwordHash;
        }
    }

    public Optional<CredentialRecord> findCredentialsByEmail(String email)
            throws SQLException {

        String sql = """
                SELECT user_id, email, password_hash, role, account_status
                FROM users
                WHERE email = ?
                """;

        try (
                Connection connection = MySQLConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, normalizeEmail(email));

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                User user = new User(
                        result.getLong("user_id"),
                        result.getString("email"),
                        result.getString("role"),
                        result.getString("account_status")
                );

                return Optional.of(new CredentialRecord(
                        user,
                        result.getString("password_hash")
                ));
            }
        }
    }

    public long countAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (
                Connection connection = MySQLConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            result.next();
            return result.getLong(1);
        }
    }

    /**
     * Initial setup only. AdminBootstrap may use this method to create the
     * first Admin. Once an Admin exists, this method refuses to add another.
     * Additional Admin accounts must be created through createAdminByActiveAdmin.
     */
    public void createAdmin(String email, String passwordHash)
            throws SQLException {

        String normalizedEmail = validateEmail(email);
        validatePasswordHash(passwordHash);

        try (Connection connection = MySQLConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                if (countAdmins(connection) > 0) {
                    throw new SQLException(
                            "Initial Admin setup is already complete. "
                                    + "Use Admin Management to add another Admin."
                    );
                }

                ensureEmailAvailable(connection, normalizedEmail);
                insertAdmin(connection, normalizedEmail, passwordHash);
                connection.commit();

            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Creates an additional Admin only when the acting account still exists,
     * is ACTIVE, and has role ADMIN in the database.
     */
    public void createAdminByActiveAdmin(
            long actingAdminUserId,
            String email,
            String passwordHash
    ) throws SQLException {

        if (actingAdminUserId <= 0) {
            throw new IllegalArgumentException("A valid Admin account is required.");
        }

        String normalizedEmail = validateEmail(email);
        validatePasswordHash(passwordHash);

        try (Connection connection = MySQLConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                requireActiveAdmin(connection, actingAdminUserId);
                ensureEmailAvailable(connection, normalizedEmail);
                insertAdmin(connection, normalizedEmail, passwordHash);
                connection.commit();

            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private long countAdmins(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            result.next();
            return result.getLong(1);
        }
    }

    private void requireActiveAdmin(Connection connection, long userId)
            throws SQLException {

        String sql = """
                SELECT role, account_status
                FROM users
                WHERE user_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()
                        || !"ADMIN".equalsIgnoreCase(result.getString("role"))
                        || !"ACTIVE".equalsIgnoreCase(
                                result.getString("account_status"))) {
                    throw new SQLException(
                            "Only a currently active Admin can create another Admin."
                    );
                }
            }
        }
    }

    private void ensureEmailAvailable(Connection connection, String email)
            throws SQLException {

        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new SQLException(
                            "An account with this email already exists."
                    );
                }
            }
        }
    }

    private void insertAdmin(
            Connection connection,
            String email,
            String passwordHash
    ) throws SQLException {

        String sql = """
                INSERT INTO users
                    (email, password_hash, role, account_status)
                VALUES (?, ?, 'ADMIN', 'ACTIVE')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        }
    }

    private static String validateEmail(String email) {
        String normalized = normalizeEmail(email);

        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        return normalized;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("A password hash is required.");
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original database exception.
        }
    }
}
