package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.dialog.ApplicationDialog;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class ApplicationPanel extends DataPanel<Application> {
    private final ApplicationService service;
    private final InternshipService internshipService;
    private final ExportService exportService;
    private final CompanyService companyService;
    private final CompanySelector companies = new CompanySelector();
    private final JComboBox<Object> statuses = new JComboBox<>();
    private boolean updating;
    private Long internshipId;
    private String internshipName;
    private long optionsRequest, formRequest;
    private record State(TableState table, Long companyId, String companyName,
                         ApplicationStatus status, Long internshipId, String internshipName) {}
    private record EditData(List<Internship> internships, Application application) {}

    public ApplicationPanel(ApplicationService service, InternshipService internshipService,
                            ExportService exportService, CompanyService companyService) {
        super("Đơn ứng tuyển", new String[]{"ID", "Công ty", "Vị trí", "Ngày ứng tuyển", "Trạng thái", "Ghi chú"},
                a -> new Object[]{a.getId(), a.getCompanyName(), a.getPosition(), a.getAppliedDate(), a.getStatus(), a.getNotes()}, Application::getId);
        this.service = service; this.internshipService = internshipService;
        this.exportService = exportService; this.companyService = companyService;
        statuses.addItem("Tất cả trạng thái");
        for (ApplicationStatus status : ApplicationStatus.values()) statuses.addItem(status);
        statuses.setToolTipText("Lọc trạng thái ứng tuyển");
        toolbar.add(companies); toolbar.add(statuses);
        companies.onChange(() -> { internshipId = null; internshipName = null; formRequest++; refresh(); });
        statuses.addActionListener(e -> { if (!updating) { formRequest++; refresh(); } });
        button("Tìm", this::refresh); button("Xóa bộ lọc", this::clearFilters);
        button("Thêm", () -> edit(null));
        button("Xuất CSV", this::export); button("Làm mới", this::refresh);
        rowActions(action("Sửa", a -> edit(a.getId())), action("Trạng thái", this::changeStatus),
                action("Xem vị trí", a -> navigator.accept(NavigationTarget.position(a))),
                action("Xóa", a -> delete("đơn #" + a.getId(), () -> service.deleteApplication(a.getId()))));
        activateRows(2, a -> edit(a.getId()));
    }

    private ApplicationStatus selectedStatus() {
        return statuses.getSelectedItem() instanceof ApplicationStatus s ? s : null;
    }
    private void setStatus(ApplicationStatus status) {
        updating = true;
        try { statuses.setSelectedItem(status == null ? "Tất cả trạng thái" : status); }
        finally { updating = false; }
    }
    private void clearFilters() {
        formRequest++; internshipId = null; internshipName = null;
        companies.select(null, null); setStatus(null); resetTableState(); refresh();
    }
    @Override public Object captureState() {
        return new State(captureTableState(), companies.selectedId(), companies.selectedName(), selectedStatus(), internshipId, internshipName);
    }
    @Override public void restoreState(Object value) {
        formRequest++;
        State s = (State) value;
        companies.select(s.companyId(), s.companyName()); setStatus(s.status());
        internshipId = s.internshipId(); internshipName = s.internshipName();
        restoreTableState(s.table()); refresh();
    }
    @Override public void openPage(NavigationTarget target) {
        formRequest++; resetTableState();
        companies.select(target.companyId(), target.companyName()); setStatus(target.status());
        internshipId = target.internshipId(); internshipName = target.internshipName();
        if (target.applicationId() != null) focusRow(target.applicationId());
        refresh();
        if (target.openForm()) {
            if (target.applicationId() != null) edit(target.applicationId());
            else if (internshipId != null) applyForInternship(internshipId);
        }
    }
    @Override public String navigationTitle() {
        String result = "Đơn ứng tuyển";
        if (companies.selectedId() != null) result = companies.selectedName() + " / " + result;
        if (internshipId != null) result += " / " + internshipName;
        if (selectedStatus() != null) result += " / " + selectedStatus();
        return result;
    }
    @Override protected String emptyMessage() {
        return internshipId != null && selectedStatus() == null && search.getText().isBlank()
                ? "Vị trí này chưa có đơn. Chọn Thêm để tạo đơn ứng tuyển."
                : super.emptyMessage();
    }

    private void edit(Long id) {
        if (id == null && internshipId != null) { applyForInternship(internshipId); return; }
        long request = ++formRequest, version = viewVersion;
        Long companyId = companies.selectedId();
        Ui.run(this, null, () -> {
            if (id != null) {
                Application a = service.getApplicationById(id);
                return new EditData(List.of(internshipService.getInternshipById(a.getInternshipId())), a);
            }
            Set<Long> used = new HashSet<>();
            for (Application a : service.getAllApplications()) used.add(a.getInternshipId());
            List<Internship> options = internshipService.getAllInternships().stream()
                    .filter(i -> companyId == null || companyId.equals(i.getCompanyId()))
                    .filter(i -> !used.contains(i.getId())).toList();
            return new EditData(options, null);
        }, data -> showEditor(data, null, request, version));
    }

    private void applyForInternship(Long id) {
        long request = ++formRequest, version = viewVersion;
        Ui.run(this, null, () -> new EditData(List.of(internshipService.getInternshipById(id)), service.findApplicationByInternship(id)),
                data -> {
                    if (data.application() != null && isCurrentView(version) && request == formRequest) {
                        focusRow(data.application().getId()); refresh();
                    }
                    showEditor(data, id, request, version);
                });
    }

    private void showEditor(EditData data, Long defaultInternshipId, long request, long version) {
        if (!isCurrentView(version) || request != formRequest) return;
        if (data.internships().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có vị trí chưa tạo đơn trong phạm vi này. Hãy thêm vị trí ở màn Vị trí thực tập hoặc mở đơn đã có.");
            return;
        }
        ApplicationDialog dialog = new ApplicationDialog(SwingUtilities.getWindowAncestor(this), service,
                data.internships(), data.application(), defaultInternshipId);
        if (dialog.showDialog()) {
            Application saved = dialog.getSavedApplication();
            if (data.application() == null) {
                search.setText("");
                if (selectedStatus() != null && selectedStatus() != saved.getStatus()) setStatus(saved.getStatus());
            }
            focusRow(saved.getId()); refresh();
        }
    }

    private void changeStatus(Application selected) {
        Long id = selected.getId(); long version = viewVersion;
        Ui.run(this, null, () -> service.getApplicationById(id), a -> {
            if (!isCurrentView(version)) return;
            ApplicationStatus choice = (ApplicationStatus) JOptionPane.showInputDialog(this,
                    "Trạng thái hiện tại: " + a.getStatus(), "Cập nhật trạng thái",
                    JOptionPane.QUESTION_MESSAGE, null, ApplicationStatus.values(), a.getStatus());
            if (choice != null) Ui.run(this, null, () -> { service.updateStatus(id, choice); return true; }, ignored -> refresh());
        });
    }

    private void export() {
        if (loading || !loadSuccessful) throw new ValidationException("Hãy đợi danh sách tải thành công trước khi xuất CSV.");
        List<Application> snapshot = IntStream.range(0, table.getRowCount())
                .mapToObj(row -> rows.get(table.convertRowIndexToModel(row))).toList();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất danh sách đang hiển thị");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        chooser.setSelectedFile(new java.io.File("applications.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) path = Path.of(path + ".csv");
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this, "File đã tồn tại. Ghi đè?",
                "Xuất CSV", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Path target = path;
        Ui.run(this, null, () -> { exportService.exportApplicationsToCsv(snapshot, target); return target; },
                file -> JOptionPane.showMessageDialog(this, "Đã xuất " + snapshot.size() + " đơn tới:\n" + file.toAbsolutePath()));
    }

    @Override public void refresh() {
        String term = search.getText(); ApplicationStatus status = selectedStatus();
        Long companyId = companies.selectedId(), positionId = internshipId;
        context.setText(positionId == null ? "Bấm tên vị trí trong bảng để mở đơn. Có thể lọc kết hợp công ty và trạng thái."
                : "Đơn của vị trí: " + internshipName + " · Thêm sẽ mở đơn đã có hoặc tạo đơn cho vị trí này.");
        context.setToolTipText(context.getText());
        load(() -> service.searchApplications(term, status, companyId, positionId));
        long token = ++optionsRequest;
        Ui.run(this, null, companyService::getAllCompanies, options -> {
            if (token != optionsRequest) return;
            if (companies.updateOptions(options)) {
                internshipId = null; internshipName = null; refresh();
            } else navigationChanged.run();
        });
    }
}

