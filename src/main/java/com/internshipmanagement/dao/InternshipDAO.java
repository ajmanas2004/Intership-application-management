package com.internshipmanagement.dao;

import com.internshipmanagement.config.ConnectionProvider;
import com.internshipmanagement.entity.Internship;
import com.internshipmanagement.entity.DeadlineFilter;
import java.time.LocalDate;
import java.sql.*;
import java.util.*;

public class InternshipDAO {
    private static final String SELECT = "SELECT i.*, c.name AS company_name FROM internships i JOIN companies c ON c.id=i.company_id ";
    private final Jdbc jdbc;
    public InternshipDAO(ConnectionProvider connections) { jdbc = new Jdbc(connections); }
    public List<Internship> findAll() { return search("", null); }
    public List<Internship> search(String term, Long companyId) {
        return search(term, companyId, DeadlineFilter.ALL, LocalDate.now());
    }
    public List<Internship> search(String term, Long companyId, DeadlineFilter deadline, LocalDate today) {
        List<Object> values = new ArrayList<>(); values.add(Jdbc.contains(term));
        String sql = SELECT + "WHERE LOWER(i.position) LIKE LOWER(?) ESCAPE '!'";
        if (companyId != null) { sql += " AND i.company_id=?"; values.add(companyId); }
        switch (deadline) {
            case OPEN -> { sql += " AND (i.deadline IS NULL OR i.deadline>=?)"; values.add(today); }
            case SOON -> { sql += " AND i.deadline BETWEEN ? AND ?"; values.add(today); values.add(today.plusDays(7)); }
            case EXPIRED -> { sql += " AND i.deadline<?"; values.add(today); }
            case NO_DEADLINE -> sql += " AND i.deadline IS NULL";
            case ALL -> {}
        }
        return jdbc.query(sql + " ORDER BY i.id DESC", InternshipDAO::map, values.toArray());
    }
    public Internship findById(Long id) {
        return jdbc.query(SELECT + "WHERE i.id=?", InternshipDAO::map, id).stream().findFirst().orElse(null);
    }
    public long insert(Internship i) {
        return jdbc.insert("INSERT INTO internships(company_id, position, location, deadline, description) VALUES (?,?,?,?,?)",
                i.getCompanyId(), i.getPosition(), i.getLocation(), i.getDeadline(), i.getDescription());
    }
    public int update(Internship i) {
        return jdbc.update("UPDATE internships SET company_id=?, position=?, location=?, deadline=?, description=? WHERE id=?",
                i.getCompanyId(), i.getPosition(), i.getLocation(), i.getDeadline(), i.getDescription(), i.getId());
    }
    public boolean hasApplications(Long id) {
        return jdbc.query("SELECT COUNT(*) FROM applications WHERE internship_id=?", rs -> rs.getLong(1), id).getFirst() > 0;
    }
    public int delete(Long id) { return jdbc.update("DELETE FROM internships WHERE id=?", id); }

    private static Internship map(ResultSet rs) throws SQLException {
        Internship i = new Internship();
        i.setId(rs.getLong("id")); i.setCompanyId(rs.getLong("company_id")); i.setPosition(rs.getString("position"));
        i.setCompanyName(rs.getString("company_name")); i.setLocation(rs.getString("location"));
        i.setDeadline(rs.getObject("deadline", java.time.LocalDate.class)); i.setDescription(rs.getString("description"));
        return i;
    }
}

