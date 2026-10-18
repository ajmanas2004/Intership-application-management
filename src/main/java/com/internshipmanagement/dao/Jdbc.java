package com.internshipmanagement.dao;

import com.internshipmanagement.config.ConnectionProvider;
import java.sql.*;
import java.util.*;

public final class Jdbc {
    private final ConnectionProvider connections;
    public Jdbc(ConnectionProvider connections) { this.connections = connections; }
    @FunctionalInterface public interface Mapper<T> { T map(ResultSet rs) throws SQLException; }

    public <T> List<T> query(String sql, Mapper<T> mapper, Object... values) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, values);
            try (ResultSet rs = s.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) result.add(mapper.map(rs));
                return result;
            }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public int update(String sql, Object... values) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, values);
            return s.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public long insert(String sql, Object... values) {
        try (Connection c = connections.getConnection();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, values); s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Missing generated ID");
                return rs.getLong(1);
            }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private static void bind(PreparedStatement s, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
    }

    public static String contains(String input) {
        return "%" + (input == null ? "" : input.strip()).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}

