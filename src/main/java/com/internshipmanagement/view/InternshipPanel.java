package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.dialog.InternshipDialog;
import javax.swing.*;
import java.util.List;

public class InternshipPanel extends DataPanel<Internship> {
    private final InternshipService service;
    private final CompanyService companyService;
    private final CompanySelector companies = new CompanySelector();
    private final JComboBox<DeadlineFilter> deadlines = new JComboBox<>(DeadlineFilter.values());
    private boolean updating;
    private long refreshRequest;
    private record State(TableState table, Long companyId, String companyName, DeadlineFilter deadline) {}

    public InternshipPanel(InternshipService service, CompanyService companyService) {
        super("Vị trí thực tập", new String[]{"ID", "Công ty", "Vị trí", "Địa điểm", "Hạn ứng tuyển"},
                i -> new Object[]{i.getId(), i.getCompanyName(), i.getPosition(), i.getLocation(), i.getDeadline()}, Internship::getId);
        this.service = service; this.companyService = companyService;
        toolbar.add(companies); toolbar.add(deadlines); deadlines.setToolTipText("Lọc hạn ứng tuyển");
        companies.onChange(this::refresh);
        deadlines.addActionListener(e -> { if (!updating) refresh(); });
        button("Tìm", this::refresh); button("Xóa bộ lọc", this::clearFilters);
        button("Thêm", () -> edit(null));
        button("Làm mới", this::refresh);
        rowActions(action("Xem đơn", i -> navigator.accept(NavigationTarget.internship(i, false))),
                action("Ứng tuyển", i -> navigator.accept(NavigationTarget.internship(i, true))),
                action("Sửa", i -> edit(i.getId())),
                action("Xóa", i -> delete("vị trí " + i.getPosition(), () -> service.deleteInternship(i.getId()))));
        activateRows(2, i -> navigator.accept(NavigationTarget.internship(i, false)));
    }

    private void edit(Long id) {
        Long companyId = companies.selectedId();
        long version = viewVersion;
        Ui.run(this, null, () -> new EditData(companyService.getAllCompanies(), id == null ? null : service.getInternshipById(id)),
                data -> {
                    if (!isCurrentView(version)) return;
                    if (data.companies().isEmpty()) throw new ValidationException("Hãy thêm công ty trước khi tạo vị trí.");
                    if (new InternshipDialog(SwingUtilities.getWindowAncestor(this), service, data.companies(), data.internship(), companyId).showDialog()) refresh();
                });
    }
    private record EditData(List<Company> companies, Internship internship) {}

    private void setDeadline(DeadlineFilter value) {
        updating = true;
        try { deadlines.setSelectedItem(value); } finally { updating = false; }
    }
    private void clearFilters() {
        companies.select(null, null); setDeadline(DeadlineFilter.ALL); resetTableState(); refresh();
    }
    @Override public Object captureState() {
        return new State(captureTableState(), companies.selectedId(), companies.selectedName(), (DeadlineFilter) deadlines.getSelectedItem());
    }
    @Override public void restoreState(Object value) {
        State s = (State) value; companies.select(s.companyId(), s.companyName()); setDeadline(s.deadline());
        restoreTableState(s.table()); refresh();
    }
    @Override public void openPage(NavigationTarget target) {
        resetTableState(); companies.select(target.companyId(), target.companyName()); setDeadline(DeadlineFilter.ALL);
        if (target.internshipId() != null) focusRow(target.internshipId());
        refresh();
    }
    @Override public String navigationTitle() {
        return companies.selectedId() == null ? "Vị trí thực tập" : "Công ty / " + companies.selectedName() + " / Vị trí thực tập";
    }
    @Override public void refresh() {
        Long id = companies.selectedId(); String term = search.getText();
        DeadlineFilter deadline = (DeadlineFilter) deadlines.getSelectedItem();
        updateContext();
        load(() -> service.searchInternships(term, id, deadline));
        long token = ++refreshRequest;
        Ui.run(this, null, companyService::getAllCompanies, options -> {
            if (token != refreshRequest) return;
            if (companies.updateOptions(options)) refresh();
            else { updateContext(); navigationChanged.run(); }
        });
    }
    private void updateContext() {
        context.setText(companies.selectedId() == null ? "Bấm tên vị trí để xem đơn, hoặc chọn Ứng tuyển để tạo / mở đơn."
                : "Vị trí của " + companies.selectedName() + " · Thêm mới sẽ chọn sẵn công ty này.");
        context.setToolTipText(context.getText());
    }
}

