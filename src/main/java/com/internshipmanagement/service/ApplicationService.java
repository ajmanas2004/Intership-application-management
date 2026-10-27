package com.internshipmanagement.service;

import com.internshipmanagement.dao.*;
import com.internshipmanagement.entity.*;
import java.time.*;
import java.util.List;
import java.util.Objects;

public class ApplicationService {
    private final ApplicationDAO dao;
    private final InternshipDAO internships;
    private final Clock clock;
    public ApplicationService(ApplicationDAO dao, InternshipDAO internships) {
        this(dao, internships, Clock.systemDefaultZone());
    }
    public ApplicationService(ApplicationDAO dao, InternshipDAO internships, Clock clock) {
        this.dao = dao; this.internships = internships; this.clock = clock;
    }
    public List<Application> getAllApplications() { return dao.findAll(); }
    public List<Application> searchApplications(String term) { return dao.search(term, null); }
    public List<Application> searchApplications(String term, ApplicationStatus status) { return dao.search(term, status); }
    public List<Application> getApplicationsByStatus(ApplicationStatus status) { return dao.search("", status); }
    public Application getApplicationById(Long id) {
        Validation.id(id);
        Application a = dao.findById(id);
        if (a == null) throw new ValidationException("Đơn ứng tuyển không còn tồn tại.");
        return a;
    }
    public void createApplication(Application a) {
        validate(a);
        if (a.getStatus() != ApplicationStatus.SAVED && a.getStatus() != ApplicationStatus.APPLIED)
            throw new ValidationException("Đơn mới chỉ có trạng thái Đã lưu hoặc Đã ứng tuyển.");
        if (dao.existsForInternship(a.getInternshipId())) throw new ValidationException("Vị trí này đã có đơn ứng tuyển.");
        normalizeDate(a, true);
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        a.setCreatedAt(now); a.setUpdatedAt(now); a.setId(dao.insert(a));
    }
    public void updateApplication(Application a) {
        validate(a);
        Application old = getApplicationById(a.getId());
        if (!Objects.equals(old.getInternshipId(), a.getInternshipId()))
            throw new ValidationException("Không thể đổi vị trí của đơn đã tạo. Hãy tạo đơn mới cho vị trí khác.");
        if (!old.getStatus().canTransitionTo(a.getStatus()))
            throw new ValidationException("Không thể chuyển từ " + old.getStatus() + " sang " + a.getStatus() + ".");
        normalizeDate(a, old.getStatus() == ApplicationStatus.SAVED);
        a.setCreatedAt(old.getCreatedAt()); a.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        Validation.changed(dao.update(a, old.getStatus()));
    }
    public void updateStatus(Long id, ApplicationStatus status) {
        Application a = getApplicationById(id); a.setStatus(status); updateApplication(a);
    }
    public void deleteApplication(Long id) { Validation.id(id); Validation.changed(dao.delete(id)); }

    private void validate(Application a) {
        if (a == null) throw new ValidationException("Thông tin đơn không hợp lệ.");
        Validation.id(a.getInternshipId());
        if (internships.findById(a.getInternshipId()) == null) throw new ValidationException("Vị trí thực tập không còn tồn tại.");
        if (a.getStatus() == null) throw new ValidationException("Hãy chọn trạng thái ứng tuyển.");
        a.setNotes(Validation.text(a.getNotes(), "Ghi chú"));
    }
    private void normalizeDate(Application a, boolean allowDefault) {
        if (a.getStatus() == ApplicationStatus.SAVED) {
            if (a.getAppliedDate() != null) throw new ValidationException("Đơn Đã lưu chưa có ngày ứng tuyển.");
        } else {
            if (a.getAppliedDate() == null && allowDefault) a.setAppliedDate(LocalDate.now(clock));
            if (a.getAppliedDate() == null) throw new ValidationException("Ngày ứng tuyển không được để trống.");
            if (a.getAppliedDate().getYear() < 1000 || a.getAppliedDate().isAfter(LocalDate.now(clock)))
                throw new ValidationException("Ngày ứng tuyển phải từ năm 1000 và không ở tương lai.");
        }
    }
}

