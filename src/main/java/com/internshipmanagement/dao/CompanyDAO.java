package com.internshipmanagement.dao;

import com.internshipmanagement.config.ConnectionProvider;
import com.internshipmanagement.entity.Company;
import java.sql.*;
import java.util.List;

public class CompanyDAO {
    private final Jdbc jdbc;
    public CompanyDAO(ConnectionProvider connections) { jdbc = new Jdbc(connections); }

    public List<Company> findAll() { return search(""); }
    public List<Company> search(String term) {
        return search(term, null, null);
    }
    public List<Company> search(String term, String industry, String location) {
        java.util.ArrayList<Object> values = new java.util.ArrayList<>();
        values.add(Jdbc.contains(term));
        String sql = "SELECT * FROM companies WHERE LOWER(name) LIKE LOWER(?) ESCAPE '!'";
        if (industry != null && !industry.isBlank()) { sql += " AND industry=?"; values.add(industry); }
        if (location != null && !location.isBlank()) { sql += " AND location=?"; values.add(location); }
        return jdbc.query(sql + " ORDER BY name, id", CompanyDAO::map, values.toArray());
    }
    public Company findById(Long id) {
        return jdbc.query("SELECT * FROM companies WHERE id = ?", CompanyDAO::map, id).stream().findFirst().orElse(null);
    }
    public long insert(Company c) {
        return jdbc.insert("INSERT INTO companies(name, industry, location, website) VALUES (?, ?, ?, ?)",
                c.getName(), c.getIndustry(), c.getLocation(), c.getWebsite());
    }
    public int update(Company c) {
        return jdbc.update("UPDATE companies SET name=?, industry=?, location=?, website=? WHERE id=?",
                c.getName(), c.getIndustry(), c.getLocation(), c.getWebsite(), c.getId());
    }
    public boolean hasInternships(Long id) {
        return jdbc.query("SELECT COUNT(*) FROM internships WHERE company_id=?", rs -> rs.getLong(1), id).getFirst() > 0;
    }
    public int delete(Long id) { return jdbc.update("DELETE FROM companies WHERE id=?", id); }

    private static Company map(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setId(rs.getLong("id")); c.setName(rs.getString("name")); c.setIndustry(rs.getString("industry"));
        c.setLocation(rs.getString("location")); c.setWebsite(rs.getString("website"));
        return c;
    }
}

