package com.mediflow.database;

import com.mediflow.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place that provides JDBC connections to the Mediflow MySQL database.
 * The application uses MySQL for both development and production.
 * The database schema and seed data are loaded from:
 * - database/schema.sql
 * - database/seed.sql
 */
public final class DatabaseConnection {

    private static final String MYSQL_URL =
            "jdbc:mysql://localhost:3306/mediflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "KaySQL21.";

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    MYSQL_URL,
                    MYSQL_USER,
                    MYSQL_PASSWORD
            );
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to connect to the database",
                    e
            );
        }
    }
}
