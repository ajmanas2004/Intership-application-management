package com.internshipmanagement.entity;

public class Application {
    private Long id;
    private Long internshipId;
    private java.time.LocalDate appliedDate;
    private ApplicationStatus status;
    private String notes;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private String position;
    private String companyName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInternshipId() { return internshipId; }
    public void setInternshipId(Long internshipId) { this.internshipId = internshipId; }

    public java.time.LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(java.time.LocalDate appliedDate) { this.appliedDate = appliedDate; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    @Override public String toString() { return (companyName == null ? "" : companyName + " — ") + (position == null ? "" : position); }
}

