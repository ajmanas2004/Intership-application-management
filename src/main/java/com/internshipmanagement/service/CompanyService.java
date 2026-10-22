package com.internshipmanagement.service;

import com.internshipmanagement.dao.CompanyDAO;
import com.internshipmanagement.entity.Company;
import java.util.List;

public class CompanyService {
    private final CompanyDAO dao;
    public CompanyService(CompanyDAO dao) { this.dao = dao; }
    public List<Company> getAllCompanies() { return dao.findAll(); }
    public List<Company> searchCompanies(String term) { return dao.search(term); }
    public Company getCompanyById(Long id) {
        Validation.id(id);
        Company c = dao.findById(id);
        if (c == null) throw new ValidationException("Công ty không còn tồn tại.");
        return c;
    }
    public void createCompany(Company c) { validate(c); c.setId(dao.insert(c)); }
    public void updateCompany(Company c) {
        validate(c); Validation.id(c.getId()); Validation.changed(dao.update(c));
    }
    public void deleteCompany(Long id) {
        getCompanyById(id);
        if (dao.hasInternships(id)) throw new ValidationException("Không thể xóa công ty đang có vị trí thực tập.");
        Validation.changed(dao.delete(id));
    }
    private void validate(Company c) {
        if (c == null) throw new ValidationException("Thông tin công ty không hợp lệ.");
        c.setName(Validation.required(c.getName(), "Tên công ty", 255));
        c.setIndustry(Validation.optional(c.getIndustry(), "Ngành nghề", 100));
        c.setLocation(Validation.optional(c.getLocation(), "Địa điểm", 255));
        c.setWebsite(Validation.optional(c.getWebsite(), "Website", 255));
    }
}

