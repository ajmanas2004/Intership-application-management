package com.internshipmanagement.entity;

public enum ApplicationStatus {
    SAVED("Đã lưu"), APPLIED("Đã ứng tuyển"), INTERVIEW("Phỏng vấn"),
    OFFERED("Được nhận"), REJECTED("Bị từ chối");
    private final String label;
    ApplicationStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}

