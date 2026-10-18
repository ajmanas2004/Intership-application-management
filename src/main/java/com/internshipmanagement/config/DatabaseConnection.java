package com.internshipmanagement.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

public final class DatabaseConnection implements ConnectionProvider {
    private final String url, user, password;

    public DatabaseConnection() {
        Properties p = new Properties();
        Path path = Path.of("database.properties");
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
            catch (IOException e) { throw new IllegalStateException("Không đọc được database.properties.", e); }
        }
        url = setting("DB_URL", p, "db.url", "jdbc:mysql://localhost:3306/internship_management?connectTimeout=5000&socketTimeout=15000&connectionTimeZone=Asia/Ho_Chi_Minh");
        user = setting("DB_USER", p, "db.user", "internship_app");
        password = setting("DB_PASSWORD", p, "db.password", "");
    }

    private static String setting(String env, Properties p, String key, String fallback) {
        String value = System.getenv(env);
        return value == null ? p.getProperty(key, fallback) : value;
    }

    @Override public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}

