package com.internshipmanagement.service;

import java.nio.charset.StandardCharsets;
public final class Validation {
    private Validation() {}
    public static String required(String value, String label, int max) {
        if (value == null || value.isBlank()) throw new ValidationException(label + " không được để trống.");
        return optional(value, label, max);
    }
    public static String optional(String value, String label, int max) {
        String clean = value == null ? "" : value.strip();
        if (clean.codePointCount(0, clean.length()) > max) throw new ValidationException(label + " tối đa " + max + " ký tự.");
        return clean;
    }
    public static String text(String value, String label) {
        String clean = value == null ? "" : value.strip();
        if (clean.getBytes(StandardCharsets.UTF_8).length > 65535) throw new ValidationException(label + " vượt quá kích thước cho phép.");
        return clean;
    }
    public static void id(Long id) {
        if (id == null || id <= 0) throw new ValidationException("ID không hợp lệ.");
    }
    public static void changed(int count) {
        if (count == 0) throw new ValidationException("Dữ liệu đã thay đổi hoặc không còn tồn tại. Hãy làm mới danh sách.");
    }
}

