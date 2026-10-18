package com.internshipmanagement.config;
import java.sql.*;
/** Each call returns a new connection; the caller must close it. */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}

