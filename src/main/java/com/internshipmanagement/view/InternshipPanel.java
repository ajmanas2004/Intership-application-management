package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.dialog.InternshipDialog;
import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class InternshipPanel extends DataPanel<Internship> {
    private final InternshipService service;
    private final CompanyService companyService;
    private final JComboBox<Company> companies = new JComboBox<>();
    private boolean updating;
    private long refreshRequest;

    public InternshipPanel(InternshipService service, CompanyService companyService) {
        super("Vị trí thực tập", new String[]{"ID", "Công ty", "Vị trí", "Địa điểm", "Hạn ứng tuyển"},
                i -> new Object[]{i.getId(), i.getCompanyName(), i.getPosition(), i.getLocation(), i.getDeadline()});
        this.service = service; this.companyService = companyService;
        companies.setPrototypeDisplayValue(allCompanies()); toolbar.add(companies);
        companies.addActionListener(e -> { if (!updating) refresh(); });
        button("Tìm", this::refresh); button("Thêm", () -> edit(null));
        button("Sửa", () -> edit(selected().getId()));
        button("Xóa", () -> { Internship i = selected(); delete("vị trí " + i.getPosition(), () -> service.deleteInternship(i.getId())); });
        button("Làm mới", this::refresh);
    }
    private static Company allCompanies() { Company c = new Company(); c.setName("Tất cả công ty"); return c; }

    private void edit(Long id) {
        Ui.run(this, null, () -> new EditData(companyService.getAllCompanies(), id == null ? null : service.getInternshipById(id)),
                data -> {
                    if (data.companies().isEmpty()) throw new ValidationException("Hãy thêm công ty trước khi tạo vị trí.");
                    if (new InternshipDialog(SwingUtilities.getWindowAncestor(this), service, data.companies(), data.internship()).showDialog()) refresh();
                });
    }
    private record EditData(List<Company> companies, Internship internship) {}

    @Override public void refresh() {
        long token = ++refreshRequest;
        Company selected = (Company) companies.getSelectedItem();
        Long id = selected == null ? null : selected.getId();
        String term = search.getText();
        load(() -> service.searchInternships(term, id));
        Ui.run(this, null, companyService::getAllCompanies, options -> {
            if (token != refreshRequest) return;
            updating = true;
            try {
                companies.removeAllItems(); companies.addItem(allCompanies());
                for (Company c : options) {
                    companies.addItem(c);
                    if (Objects.equals(id, c.getId())) companies.setSelectedItem(c);
                }
            } finally { updating = false; }
        });
    }
}

