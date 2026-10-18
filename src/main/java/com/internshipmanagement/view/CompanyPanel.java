package com.internshipmanagement.view;

import com.internshipmanagement.entity.Company;
import com.internshipmanagement.service.CompanyService;
import com.internshipmanagement.view.dialog.CompanyDialog;
import javax.swing.*;

public class CompanyPanel extends DataPanel<Company> {
    private final CompanyService service;

    public CompanyPanel(CompanyService service) {
        super("Công ty", new String[]{"ID", "Tên công ty", "Ngành nghề", "Địa điểm", "Website"},
                c -> new Object[]{c.getId(), c.getName(), c.getIndustry(), c.getLocation(), c.getWebsite()});
        this.service = service;
        button("Tìm", this::refresh);
        button("Thêm", () -> { if (new CompanyDialog(SwingUtilities.getWindowAncestor(this), service, null).showDialog()) refresh(); });
        button("Sửa", () -> {
            Long id = selected().getId();
            Ui.run(this, null, () -> service.getCompanyById(id), c -> {
                if (new CompanyDialog(SwingUtilities.getWindowAncestor(this), service, c).showDialog()) refresh();
            });
        });
        button("Xóa", () -> { Company c = selected(); delete("công ty " + c.getName(), () -> service.deleteCompany(c.getId())); });
        button("Làm mới", this::refresh);
    }

    @Override public void refresh() {
        String term = search.getText();
        load(() -> service.searchCompanies(term));
    }
}

