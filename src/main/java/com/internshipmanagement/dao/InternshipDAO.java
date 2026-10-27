package com.internshipmanagement.dao;

import com.internshipmanagement.config.ConnectionProvider;
import com.internshipmanagement.entity.Internship;
import java.sql.*;
import java.util.*;

public class InternshipDAO {
    private static final String SELECT = "SELECT i.*, c.name AS company_name FROM internships i JOIN companies c ON c.id=i.company_id ";
    private final Jdbc jdbc;
    public InternshipDAO(ConnectionProvider connections) { jdbc = new Jdbc(connections); }
    public List<Internship> findAll() { return search("", null); }
    public List<Internship> search(String term, Long companyId) {
        return jdbc.query(SELECT + "WHERE LOWER(i.position) LIKE LOWER(?) ESCAPE '!'"
                + (companyId == null ? "" : " AND i.company_id=?") + " ORDER BY i.id DESC",
                InternshipDAO::map, companyId == null ? new Object[]{Jdbc.contains(term)} : new Object[]{Jdbc.contains(term), companyId});
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

