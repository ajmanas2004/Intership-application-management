package com.internshipmanagement.entity;
import java.util.List;

public enum ApplicationStatus {
    SAVED("Đã lưu"), APPLIED("Đã ứng tuyển"), INTERVIEW("Phỏng vấn"),
    OFFERED("Được nhận"), REJECTED("Bị từ chối");
    private final String label;
    ApplicationStatus(String label) { this.label = label; }
    public List<ApplicationStatus> nextStatuses() {
        return switch (this) {
            case SAVED -> List.of(APPLIED);
            case APPLIED -> List.of(INTERVIEW, REJECTED);
            case INTERVIEW -> List.of(OFFERED, REJECTED);
            case OFFERED, REJECTED -> List.of();
        };
    }
    public boolean canTransitionTo(ApplicationStatus next) { return this == next || nextStatuses().contains(next); }
    @Override public String toString() { return label; }
}

