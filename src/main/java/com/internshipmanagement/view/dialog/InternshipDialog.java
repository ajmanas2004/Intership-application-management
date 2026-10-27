package com.internshipmanagement.view.dialog;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.*;
import javax.swing.*;
import java.awt.Window;
import java.util.List;

public class InternshipDialog extends FormDialog {
    private final InternshipService service;
    private final Long id;
    private final JComboBox<Company> companies;
    private final JTextField position, location;
    private final JFormattedTextField deadline;
    private final JTextArea description;

    public InternshipDialog(Window owner, InternshipService service, List<Company> options, Internship existing) {
        super(owner, existing == null ? "Thêm vị trí thực tập" : "Sửa vị trí thực tập");
        this.service = service;
        Internship i = existing == null ? new Internship() : existing;
        id = i.getId(); companies = new JComboBox<>(options.toArray(Company[]::new));
        if (i.getCompanyId() != null)
            for (Company c : options) if (c.getId().equals(i.getCompanyId())) companies.setSelectedItem(c);
        position = new JTextField(i.getPosition(), 28); location = new JTextField(i.getLocation(), 28);
        deadline = new JFormattedTextField(); deadline.setText(showDate(i.getDeadline()));
        description = textArea(i.getDescription());
        field("Công ty *", companies); field("Tên vị trí *", position); field("Địa điểm", location);
        field("Hạn ứng tuyển (yyyy-MM-dd)", deadline); field("Mô tả", new JScrollPane(description));
    }

    @Override protected Runnable saveAction() {
        Company company = (Company) companies.getSelectedItem();
        if (company == null) throw new ValidationException("Hãy tạo và chọn công ty trước.");
        Internship i = new Internship(); i.setId(id); i.setCompanyId(company.getId());
        i.setPosition(position.getText()); i.setLocation(location.getText());
        i.setDeadline(date(deadline, "Hạn ứng tuyển")); i.setDescription(description.getText());
        return () -> { if (id == null) service.createInternship(i); else service.updateInternship(i); };
    }
}

