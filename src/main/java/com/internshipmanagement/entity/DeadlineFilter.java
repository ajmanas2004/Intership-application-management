package com.internshipmanagement.entity;

public enum DeadlineFilter {
    ALL("Tất cả hạn nộp"), OPEN("Còn hạn / chưa có hạn"), SOON("Hạn trong 7 ngày"),
    EXPIRED("Đã hết hạn"), NO_DEADLINE("Chưa có hạn");
    private final String label;
    DeadlineFilter(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
