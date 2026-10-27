package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.dialog.ApplicationDialog;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

public class ApplicationPanel extends DataPanel<Application> {
    private final ApplicationService service;
    private final InternshipService internshipService;
    private final ExportService exportService;
    private final JComboBox<Object> statuses = new JComboBox<>();

    public ApplicationPanel(ApplicationService service, InternshipService internshipService, ExportService exportService) {
        super("Đơn ứng tuyển", new String[]{"ID", "Công ty", "Vị trí", "Ngày ứng tuyển", "Trạng thái", "Ghi chú"},
                a -> new Object[]{a.getId(), a.getCompanyName(), a.getPosition(), a.getAppliedDate(), a.getStatus(), a.getNotes()});
        this.service = service; this.internshipService = internshipService; this.exportService = exportService;
        statuses.addItem("Tất cả trạng thái");
        for (ApplicationStatus status : ApplicationStatus.values()) statuses.addItem(status);
        toolbar.add(statuses); statuses.addActionListener(e -> refresh());
        button("Tìm", this::refresh); button("Thêm", () -> edit(null));
        button("Sửa", () -> edit(selected().getId()));
        button("Xóa", () -> { Application a = selected(); delete("đơn #" + a.getId(), () -> service.deleteApplication(a.getId())); });
        button("Cập nhật trạng thái", this::changeStatus);
        button("Xuất CSV", this::export); button("Làm mới", this::refresh);
    }

    private record EditData(List<Internship> internships, Application application) {}
    private void edit(Long id) {
        Ui.run(this, null, () -> new EditData(internshipService.getAllInternships(), id == null ? null : service.getApplicationById(id)),
                data -> {
                    if (data.internships().isEmpty()) throw new ValidationException("Hãy thêm vị trí thực tập trước.");
                    if (new ApplicationDialog(SwingUtilities.getWindowAncestor(this), service, data.internships(), data.application()).showDialog()) refresh();
                });
    }

    private void changeStatus() {
        Long id = selected().getId();
        Ui.run(this, null, () -> service.getApplicationById(id), a -> {
            List<ApplicationStatus> next = a.getStatus().nextStatuses();
            if (next.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Đơn đã kết thúc ở trạng thái " + a.getStatus() + "."); return;
            }
            ApplicationStatus choice = (ApplicationStatus) JOptionPane.showInputDialog(this,
                    "Trạng thái hiện tại: " + a.getStatus(), "Cập nhật trạng thái",
                    JOptionPane.QUESTION_MESSAGE, null, next.toArray(), next.getFirst());
            if (choice != null) Ui.run(this, null, () -> { service.updateStatus(id, choice); return true; }, ignored -> refresh());
        });
    }

    private void export() {
        List<Application> snapshot = List.copyOf(rows);
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
        String term = search.getText();
        Object choice = statuses.getSelectedItem();
        ApplicationStatus status = choice instanceof ApplicationStatus s ? s : null;
        load(() -> service.searchApplications(term, status));
    }
}

