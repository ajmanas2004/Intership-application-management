package com.internshipmanagement.dao;
import java.sql.SQLException;

public class DataAccessException extends RuntimeException {
    public DataAccessException(SQLException cause) { super(message(cause), cause); }
    private static String message(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("23"))
            return "Dữ liệu bị trùng hoặc còn liên kết. Hãy làm mới danh sách và kiểm tra lại.";
        if (state != null && (state.startsWith("08") || state.equals("28000")))
            return "Không kết nối được MySQL. Kiểm tra dịch vụ, database.properties và quyền truy cập.";
        return "Không thể xử lý dữ liệu. Kiểm tra kết nối và schema database.";
    }
}

