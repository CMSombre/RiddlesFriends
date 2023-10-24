package me.cmriddles.riddlesfriends;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseHandler(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}