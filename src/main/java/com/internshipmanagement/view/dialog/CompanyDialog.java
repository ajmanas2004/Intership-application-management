package com.internshipmanagement.view.dialog;

import com.internshipmanagement.entity.Company;
import com.internshipmanagement.service.CompanyService;
import javax.swing.*;
import java.awt.Window;

public class CompanyDialog extends FormDialog {
    private final CompanyService service;
    private final Long id;
    private final JTextField name, industry, location, website;

    public CompanyDialog(Window owner, CompanyService service, Company existing) {
        super(owner, existing == null ? "Thêm công ty" : "Sửa công ty");
        this.service = service;
        Company c = existing == null ? new Company() : existing;
        id = c.getId();
        name = new JTextField(c.getName(), 28); industry = new JTextField(c.getIndustry(), 28);
        location = new JTextField(c.getLocation(), 28); website = new JTextField(c.getWebsite(), 28);
        field("Tên công ty *", name); field("Ngành nghề", industry); field("Địa điểm", location); field("Website", website);
    }

    @Override protected Runnable saveAction() {
        Company c = new Company();
        c.setId(id); c.setName(name.getText()); c.setIndustry(industry.getText());
        c.setLocation(location.getText()); c.setWebsite(website.getText());
        return () -> { if (id == null) service.createCompany(c); else service.updateCompany(c); };
    }
}

