package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;

/** Immutable destination; names are display hints, all filtering uses database IDs. */
public record NavigationTarget(String screen, Long companyId, String companyName,
                               Long internshipId, String internshipName,
                               ApplicationStatus status, Long applicationId, boolean openForm) {
    public static NavigationTarget screen(String key) {
        return new NavigationTarget(key, null, null, null, null, null, null, false);
    }
    public static NavigationTarget company(Company company) {
        return new NavigationTarget("internships", company.getId(), company.getName(), null, null, null, null, false);
    }
    public static NavigationTarget internship(Internship internship, boolean apply) {
        return new NavigationTarget("applications", internship.getCompanyId(), internship.getCompanyName(),
                internship.getId(), internship.getPosition(), null, null, apply);
    }
    public static NavigationTarget applications(ApplicationStatus status) {
        return new NavigationTarget("applications", null, null, null, null, status, null, false);
    }
    public static NavigationTarget application(Application application) {
        return new NavigationTarget("applications", null, null, null, null, null, application.getId(), true);
    }
    public static NavigationTarget position(Application application) {
        return new NavigationTarget("internships", application.getCompanyId(), application.getCompanyName(),
                application.getInternshipId(), application.getPosition(), null, null, false);
    }
}
