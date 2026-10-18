package com.internshipmanagement.dao;

import com.internshipmanagement.config.ConnectionProvider;
import com.internshipmanagement.entity.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ApplicationDAO {
    private static final String SELECT = "SELECT a.*, i.company_id, i.position, c.name AS company_name FROM applications a "
            + "JOIN internships i ON i.id=a.internship_id JOIN companies c ON c.id=i.company_id ";
    private final Jdbc jdbc;
    public ApplicationDAO(ConnectionProvider connections) { jdbc = new Jdbc(connections); }
    public List<Application> findAll() { return search("", null); }
    public Application findById(Long id) {
        return jdbc.query(SELECT + "WHERE a.id=?", ApplicationDAO::map, id).stream().findFirst().orElse(null);
    }
    public boolean existsForInternship(Long internshipId) {
        return jdbc.query("SELECT COUNT(*) FROM applications WHERE internship_id=?", rs -> rs.getLong(1), internshipId).getFirst() > 0;
    }
    public Application findByInternship(Long internshipId) {
        return jdbc.query(SELECT + "WHERE a.internship_id=?", ApplicationDAO::map, internshipId).stream().findFirst().orElse(null);
    }
    public List<Application> search(String term, ApplicationStatus status) {
        return search(term, status, null, null);
    }
    public List<Application> search(String term, ApplicationStatus status, Long companyId, Long internshipId) {
        String like = Jdbc.contains(term);
        List<Object> values = new ArrayList<>(List.of(like, like, like));
        String sql = SELECT + "WHERE (LOWER(c.name) LIKE LOWER(?) ESCAPE '!' OR LOWER(i.position) LIKE LOWER(?) ESCAPE '!'"
                + " OR LOWER(COALESCE(a.notes,'')) LIKE LOWER(?) ESCAPE '!')";
        if (status != null) { sql += " AND a.status=?"; values.add(status.name()); }
        if (companyId != null) { sql += " AND i.company_id=?"; values.add(companyId); }
        if (internshipId != null) { sql += " AND a.internship_id=?"; values.add(internshipId); }
        return jdbc.query(sql + " ORDER BY a.updated_at DESC, a.id DESC", ApplicationDAO::map, values.toArray());
    }
    public long insert(Application a) {
        return jdbc.insert("INSERT INTO applications(internship_id,applied_date,status,notes,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                a.getInternshipId(), a.getAppliedDate(), a.getStatus().name(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
    }
    /** Status comparison protects transitions if another client changes the record after validation. */
    public int update(Application a, ApplicationStatus expectedStatus) {
        return jdbc.update("UPDATE applications SET applied_date=?,status=?,notes=?,updated_at=? WHERE id=? AND internship_id=? AND status=?",
                a.getAppliedDate(), a.getStatus().name(), a.getNotes(), a.getUpdatedAt(),
                a.getId(), a.getInternshipId(), expectedStatus.name());
    }
    public int delete(Long id) { return jdbc.update("DELETE FROM applications WHERE id=?", id); }
    public long countTotal() { return jdbc.query("SELECT COUNT(*) FROM applications", rs -> rs.getLong(1)).getFirst(); }
    public long countSubmitted() {
        return jdbc.query("SELECT COUNT(*) FROM applications WHERE status <> 'SAVED'", rs -> rs.getLong(1)).getFirst();
    }
    public Map<ApplicationStatus, Long> countByStatus() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) counts.put(status, 0L);
        jdbc.query("SELECT status,COUNT(*) AS total FROM applications GROUP BY status",
                rs -> Map.entry(ApplicationStatus.valueOf(rs.getString("status")), rs.getLong("total")))
                .forEach(e -> counts.put(e.getKey(), e.getValue()));
        return counts;
    }
    public List<Application> recent(int limit) {
        return jdbc.query(SELECT + "ORDER BY a.updated_at DESC,a.id DESC LIMIT ?", ApplicationDAO::map, limit);
    }
    private static Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setId(rs.getLong("id")); a.setInternshipId(rs.getLong("internship_id"));
        a.setCompanyId(rs.getLong("company_id"));
        a.setAppliedDate(rs.getObject("applied_date", LocalDate.class));
        a.setStatus(ApplicationStatus.valueOf(rs.getString("status"))); a.setNotes(rs.getString("notes"));
        a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class)); a.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        a.setCompanyName(rs.getString("company_name")); a.setPosition(rs.getString("position"));
        return a;
    }
}

