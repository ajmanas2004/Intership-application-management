package com.internshipmanagement.view;

import com.internshipmanagement.entity.Company;
import com.internshipmanagement.service.CompanyService;
import com.internshipmanagement.view.dialog.CompanyDialog;
import javax.swing.*;

public class CompanyPanel extends DataPanel<Company> {
    private final CompanyService service;
    private final TextFilter industries = new TextFilter("Tất cả ngành nghề");
    private final TextFilter locations = new TextFilter("Tất cả địa điểm");
    private long optionsRequest;
    private record State(TableState table, String industry, String location) {}

    public CompanyPanel(CompanyService service) {
        super("Công ty", new String[]{"ID", "Tên công ty", "Ngành nghề", "Địa điểm", "Website"},
                c -> new Object[]{c.getId(), c.getName(), c.getIndustry(), c.getLocation(), c.getWebsite()}, Company::getId);
        this.service = service;
        context.setText("Bấm tên công ty để xem vị trí thực tập; các thao tác khác nằm trong cột Hành động.");
        industries.setToolTipText("Lọc ngành nghề"); locations.setToolTipText("Lọc địa điểm");
        toolbar.add(industries); toolbar.add(locations);
        industries.onChange(this::refresh); locations.onChange(this::refresh);
        button("Tìm", this::refresh); button("Xóa bộ lọc", this::clearFilters);
        button("Thêm", () -> { if (new CompanyDialog(SwingUtilities.getWindowAncestor(this), service, null).showDialog()) refresh(); });
        button("Làm mới", this::refresh);
        rowActions(action("Xem vị trí", c -> navigator.accept(NavigationTarget.company(c))),
                action("Sửa", this::edit),
                action("Xóa", c -> delete("công ty " + c.getName(), () -> service.deleteCompany(c.getId()))));
        activateRows(1, c -> navigator.accept(NavigationTarget.company(c)));
    }
    private void edit(Company selected) {
        long version = viewVersion;
        Ui.run(this, null, () -> service.getCompanyById(selected.getId()), c -> {
            if (isCurrentView(version) && new CompanyDialog(SwingUtilities.getWindowAncestor(this), service, c).showDialog()) refresh();
        });
    }
    private void clearFilters() {
        industries.select(null); locations.select(null); resetTableState(); refresh();
    }
    @Override public Object captureState() { return new State(captureTableState(), industries.value(), locations.value()); }
    @Override public void restoreState(Object value) {
        State state = (State) value;
        industries.select(state.industry()); locations.select(state.location());
        restoreTableState(state.table()); refresh();
    }
    @Override public void openPage(NavigationTarget target) { clearFilters(); }
    @Override public void refresh() {
        String term = search.getText(), industry = industries.value(), location = locations.value();
        load(() -> service.searchCompanies(term, industry, location));
        long token = ++optionsRequest;
        Ui.run(this, null, service::getAllCompanies, all -> {
            if (token != optionsRequest) return;
            boolean reset = industries.updateOptions(all.stream().map(Company::getIndustry).toList());
            reset |= locations.updateOptions(all.stream().map(Company::getLocation).toList());
            if (reset) refresh();
        });
    }
}

