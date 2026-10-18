package com.internshipmanagement.service;

import com.internshipmanagement.dao.*;
import com.internshipmanagement.entity.Internship;
import com.internshipmanagement.entity.DeadlineFilter;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class InternshipService {
    private final InternshipDAO dao;
    private final CompanyDAO companies;
    private final Clock clock;
    public InternshipService(InternshipDAO dao, CompanyDAO companies) { this(dao, companies, Clock.systemDefaultZone()); }
    public InternshipService(InternshipDAO dao, CompanyDAO companies, Clock clock) {
        this.dao = dao; this.companies = companies; this.clock = clock;
    }
    public List<Internship> getAllInternships() { return dao.findAll(); }
    public List<Internship> getInternshipsByCompany(Long id) { Validation.id(id); return dao.search("", id); }
    public List<Internship> searchInternships(String term) { return dao.search(term, null); }
    public List<Internship> searchInternships(String term, Long companyId) { return dao.search(term, companyId); }
    public List<Internship> searchInternships(String term, Long companyId, DeadlineFilter deadline) {
        return dao.search(term, companyId, deadline == null ? DeadlineFilter.ALL : deadline, LocalDate.now(clock));
    }
    public Internship getInternshipById(Long id) {
        Validation.id(id);
        Internship i = dao.findById(id);
        if (i == null) throw new ValidationException("Vị trí thực tập không còn tồn tại.");
        return i;
    }
    public void createInternship(Internship i) { validate(i); i.setId(dao.insert(i)); }
    public void updateInternship(Internship i) {
        validate(i); Validation.id(i.getId()); Validation.changed(dao.update(i));
    }
    public void deleteInternship(Long id) {
        getInternshipById(id);
        if (dao.hasApplications(id)) throw new ValidationException("Không thể xóa vị trí đã có đơn ứng tuyển.");
        Validation.changed(dao.delete(id));
    }
    private void validate(Internship i) {
        if (i == null) throw new ValidationException("Thông tin vị trí không hợp lệ.");
        Validation.id(i.getCompanyId());
        if (companies.findById(i.getCompanyId()) == null) throw new ValidationException("Công ty được chọn không còn tồn tại.");
        i.setPosition(Validation.required(i.getPosition(), "Tên vị trí", 255));
        i.setLocation(Validation.optional(i.getLocation(), "Địa điểm", 255));
        i.setDescription(Validation.text(i.getDescription(), "Mô tả"));
        if (i.getDeadline() != null && (i.getDeadline().getYear() < 1000 || i.getDeadline().getYear() > 9999))
            throw new ValidationException("Hạn ứng tuyển phải có năm từ 1000 đến 9999.");
    }
}

