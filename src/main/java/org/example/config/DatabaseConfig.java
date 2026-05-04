package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String URL = "jdbc:mysql://localhost:3306/symfony_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            seedAdmin(connection);
        }
        return connection;
    }

    private static void seedAdmin(Connection conn) {
        try {
            // Check if admin already exists
            var check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = 'admin@smartpfe.com'");
            var rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                var ps = conn.prepareStatement(
                    "INSERT INTO users (email, password, role, name, phone, is_active, is_verified, created_at, updated_at) " +
                    "VALUES ('admin@smartpfe.com', 'admin123', 'admin', 'Admin', '', 1, 1, NOW(), NOW())");
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
