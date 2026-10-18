package com.internshipmanagement.service;

import com.internshipmanagement.entity.Application;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class ExportService {
    public void exportApplicationsToCsv(List<Application> applications, Path destination) throws IOException {
        if (applications == null || destination == null) throw new ValidationException("Danh sách hoặc đường dẫn xuất không hợp lệ.");
        Path target = destination.toAbsolutePath().normalize();
        Path temp = Files.createTempFile(target.getParent(), ".applications-", ".csv");
        try {
            try (BufferedWriter w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                w.write('\uFEFF'); // Excel recognizes UTF-8 Vietnamese text.
                row(w, "ID", "Công ty", "Vị trí", "Ngày ứng tuyển", "Trạng thái", "Ghi chú", "Ngày tạo", "Cập nhật");
                for (Application a : applications)
                    row(w, a.getId(), a.getCompanyName(), a.getPosition(), a.getAppliedDate(),
                            a.getStatus() == null ? "" : a.getStatus().name(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
            }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private static void row(Writer w, Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) w.write(',');
            String text = values[i] == null ? "" : values[i].toString();
            String stripped = text.stripLeading();
            if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0
                    || text.startsWith("\t") || text.startsWith("\r") || text.startsWith("\n"))
                text = "'" + text;
            w.write('"'); w.write(text.replace("\"", "\"\"")); w.write('"');
        }
        w.write("\r\n");
    }
}

