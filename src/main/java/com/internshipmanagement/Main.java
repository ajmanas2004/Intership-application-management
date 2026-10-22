package com.internshipmanagement;

import com.internshipmanagement.config.DatabaseConnection;
import com.internshipmanagement.view.*;
import javax.swing.*;
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                new DatabaseConnection();
                MainFrame frame = new MainFrame();
                for (String screen : new String[]{"Tổng quan", "Công ty", "Vị trí thực tập", "Đơn ứng tuyển"}) {
                    JPanel p = new JPanel(); p.add(new JLabel(screen + " — nền tảng đã sẵn sàng"));
                    frame.addScreen(screen, screen, p);
                }
                frame.setVisible(true);
            } catch (Exception e) { Ui.error(null, e); }
        });
    }
}

