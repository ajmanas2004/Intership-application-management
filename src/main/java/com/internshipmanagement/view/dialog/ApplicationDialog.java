package com.internshipmanagement.view.dialog;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.DatePicker;
import java.time.LocalDate;
import javax.swing.*;
import java.awt.Window;
import java.util.*;

public class ApplicationDialog extends FormDialog {
    private final ApplicationService service;
    private final Long id;
    private final JComboBox<Internship> internships;
    private final JComboBox<ApplicationStatus> statuses;
    private final DatePicker appliedDate = new DatePicker(null);
    private LocalDate submittedDate;
    private final JTextArea notes;
    private Application savedApplication;

    public ApplicationDialog(Window owner, ApplicationService service, List<Internship> options, Application existing) {
        this(owner, service, options, existing, null);
    }
    public ApplicationDialog(Window owner, ApplicationService service, List<Internship> options, Application existing, Long defaultInternshipId) {
        super(owner, existing == null ? "Tạo đơn ứng tuyển" : "Sửa đơn ứng tuyển");
        this.service = service;
        id = existing == null ? null : existing.getId();
        internships = new JComboBox<>(options.toArray(Internship[]::new));
        Internship prototype = new Internship(); prototype.setPosition("Công ty — Vị trí thực tập");
        internships.setPrototypeDisplayValue(prototype);
        if (existing == null && defaultInternshipId != null)
            for (Internship i : options) if (i.getId().equals(defaultInternshipId)) internships.setSelectedItem(i);
        if (existing != null) {
            for (Internship i : options) if (i.getId().equals(existing.getInternshipId())) internships.setSelectedItem(i);
            internships.setEnabled(false);
        }
        statuses = new JComboBox<>(ApplicationStatus.values());
        appliedDate.setMaximumDate(LocalDate.now());
        if (existing != null) {
            statuses.setSelectedItem(existing.getStatus());
            submittedDate = existing.getAppliedDate(); appliedDate.setDate(submittedDate);
        }
        statuses.addActionListener(e -> syncDate());
        if (existing == null && defaultInternshipId != null) statuses.setSelectedItem(ApplicationStatus.APPLIED);
        notes = textArea(existing == null ? "" : existing.getNotes());
        field("Vị trí thực tập *", internships); field("Trạng thái *", statuses);
        field("Ngày ứng tuyển", appliedDate);
        field("Ghi chú", new JScrollPane(notes));
        field(" ", new JLabel("Chọn ngày từ lịch; đơn Đã lưu chưa có ngày ứng tuyển."));
        syncDate();
    }

    private void syncDate() {
        boolean saved = statuses.getSelectedItem() == ApplicationStatus.SAVED;
        if (saved) {
            if (appliedDate.getDate() != null) submittedDate = appliedDate.getDate();
            appliedDate.setDate(null);
        } else if (appliedDate.getDate() == null) {
            appliedDate.setDate(submittedDate == null ? LocalDate.now() : submittedDate);
        }
        appliedDate.setEnabled(!saved);
    }

    @Override protected Runnable saveAction() {
        Internship internship = (Internship) internships.getSelectedItem();
        if (internship == null) throw new ValidationException("Hãy tạo vị trí thực tập trước.");
        Application a = new Application(); a.setId(id); a.setInternshipId(internship.getId());
        a.setStatus((ApplicationStatus) statuses.getSelectedItem());
        a.setAppliedDate(appliedDate.getDate()); a.setNotes(notes.getText());
        savedApplication = a;
        return () -> { if (id == null) service.createApplication(a); else service.updateApplication(a); };
    }
    public Application getSavedApplication() { return savedApplication; }
}

