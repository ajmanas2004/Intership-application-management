package com.internshipmanagement.service;

import com.internshipmanagement.dao.ApplicationDAO;
import com.internshipmanagement.entity.*;
import java.util.*;

public class DashboardService {
    private final ApplicationDAO dao;
    public DashboardService(ApplicationDAO dao) { this.dao = dao; }
    public long countTotalApplications() { return dao.countTotal(); }
    public Map<ApplicationStatus, Long> countApplicationsByStatus() { return dao.countByStatus(); }
    public long countSubmittedApplications() { return dao.countSubmitted(); }
    public List<Application> getRecentApplications() { return dao.recent(10); }

    public Snapshot snapshot() {
        Map<ApplicationStatus, Long> counts = countApplicationsByStatus();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new Snapshot(total, total - counts.get(ApplicationStatus.SAVED), Map.copyOf(counts), getRecentApplications());
    }
    public record Snapshot(long total, long submitted, Map<ApplicationStatus, Long> counts, List<Application> recent) {}
}

