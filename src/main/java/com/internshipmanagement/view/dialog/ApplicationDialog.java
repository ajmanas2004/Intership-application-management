package com.internshipmanagement.view.dialog;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import javax.swing.*;
import java.awt.Window;
import java.util.*;

public class ApplicationDialog extends FormDialog {
    private final ApplicationService service;
    private final Long id;
    private final JComboBox<Internship> internships;
    private final JComboBox<ApplicationStatus> statuses;
    private final JFormattedTextField appliedDate = new JFormattedTextField();
    private final JTextArea notes;

    public ApplicationDialog(Window owner, ApplicationService service, List<Internship> options, Application existing) {
        super(owner, existing == null ? "Tạo đơn ứng tuyển" : "Sửa đơn ứng tuyển");
        this.service = service;
        id = existing == null ? null : existing.getId();
        internships = new JComboBox<>(options.toArray(Internship[]::new));
        if (existing != null) {
            for (Internship i : options) if (i.getId().equals(existing.getInternshipId())) internships.setSelectedItem(i);
            internships.setEnabled(false);
        }
        List<ApplicationStatus> allowed = new ArrayList<>();
        if (existing == null) allowed.addAll(List.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED));
        else {
            allowed.add(existing.getStatus()); allowed.addAll(existing.getStatus().nextStatuses());
            appliedDate.setText(showDate(existing.getAppliedDate()));
        }
        statuses = new JComboBox<>(allowed.toArray(ApplicationStatus[]::new));
        statuses.addActionListener(e -> syncDate());
        notes = textArea(existing == null ? "" : existing.getNotes());
        field("Vị trí thực tập *", internships); field("Trạng thái *", statuses);
        field("Ngày ứng tuyển (yyyy-MM-dd)", appliedDate);
        field("Ghi chú", new JScrollPane(notes));
        field(" ", new JLabel("Để trống ngày khi mới ứng tuyển: tự lấy ngày hôm nay."));
        syncDate();
    }

    private void syncDate() {
        boolean saved = statuses.getSelectedItem() == ApplicationStatus.SAVED;
        appliedDate.setEnabled(!saved);
        if (saved) appliedDate.setText("");
    }

    @Override protected Runnable saveAction() {
        Internship internship = (Internship) internships.getSelectedItem();
        if (internship == null) throw new ValidationException("Hãy tạo vị trí thực tập trước.");
        Application a = new Application(); a.setId(id); a.setInternshipId(internship.getId());
        a.setStatus((ApplicationStatus) statuses.getSelectedItem());
        a.setAppliedDate(date(appliedDate, "Ngày ứng tuyển")); a.setNotes(notes.getText());
        return () -> { if (id == null) service.createApplication(a); else service.updateApplication(a); };
    }
}

