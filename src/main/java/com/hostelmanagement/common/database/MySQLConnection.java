package com.hostelmanagement.common.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySQLConnection {

    private static final String URL = System.getenv().getOrDefault(
            "HOSTEL_DB_URL",
            "jdbc:mysql://127.0.0.1:3306/hostel_management?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000"
    );

    private static final String USER = System.getenv().getOrDefault(
            "HOSTEL_DB_USER",
            "hostel_app"
    );

    private MySQLConnection() {
        // Utility class: objects are not required.
    }

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("HOSTEL_DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException(
                    "HOSTEL_DB_PASSWORD is not configured."
            );
        }

        return DriverManager.getConnection(URL, USER, password);
    }
}