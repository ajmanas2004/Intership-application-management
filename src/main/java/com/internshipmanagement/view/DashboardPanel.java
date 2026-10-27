package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.DashboardService;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;

public class DashboardPanel extends JPanel implements Refreshable {
    private final DashboardService service;
    private final JLabel total = number();
    private final JLabel submitted = number();
    private final Map<ApplicationStatus, JLabel> counts = new EnumMap<>(ApplicationStatus.class);
    private final DefaultTableModel recent = new DefaultTableModel(new String[]{"Công ty", "Vị trí", "Trạng thái", "Ngày ứng tuyển", "Cập nhật"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel message = new JLabel(" ");
    private long request;

    public DashboardPanel(DashboardService service) {
        super(new BorderLayout(12, 16)); this.service = service;
        setBorder(new EmptyBorder(12, 20, 16, 20));
        JPanel top = new JPanel(new BorderLayout(12, 16));
        JPanel heading = new JPanel(new BorderLayout());
        heading.add(Ui.title("Tổng quan"), BorderLayout.WEST);
        JButton refresh = new JButton("Làm mới"); refresh.addActionListener(e -> refresh());
        heading.add(refresh, BorderLayout.EAST); top.add(heading, BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(2, 4, 12, 12));
        cards.add(card("Tổng đơn theo dõi", total)); cards.add(card("Tổng đã ứng tuyển", submitted));
        for (ApplicationStatus status : ApplicationStatus.values()) {
            JLabel value = number(); counts.put(status, value); cards.add(card(status.toString(), value));
        }
        cards.add(new JLabel("SAVED → APPLIED → INTERVIEW"));
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        JPanel list = new JPanel(new BorderLayout(8, 8));
        list.add(new JLabel("10 đơn cập nhật gần đây"), BorderLayout.NORTH);
        JTable table = new JTable(recent); table.setRowHeight(28); table.setAutoCreateRowSorter(true);
        list.add(new JScrollPane(table), BorderLayout.CENTER); add(list, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
    }
    private static JLabel number() {
        JLabel label = new JLabel("—"); label.setFont(label.getFont().deriveFont(Font.BOLD, 30f)); return label;
    }
    private static JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(4, 12));
        panel.setBackground(Color.WHITE); panel.setBorder(new CompoundBorder(new LineBorder(new Color(220, 225, 230)), new EmptyBorder(12, 14, 12, 14)));
        panel.add(new JLabel(title), BorderLayout.NORTH); panel.add(value, BorderLayout.CENTER); return panel;
    }
    @Override public void refresh() {
        long token = ++request; message.setText("Đang tải thống kê...");
        new SwingWorker<DashboardService.Snapshot, Void>() {
            @Override protected DashboardService.Snapshot doInBackground() { return service.snapshot(); }
            @Override protected void done() {
                if (token != request) return;
                try {
                    DashboardService.Snapshot s = get();
                    total.setText(Long.toString(s.total())); submitted.setText(Long.toString(s.submitted()));
                    counts.forEach((key, label) -> label.setText(s.counts().get(key).toString()));
                    recent.setRowCount(0);
                    for (Application a : s.recent()) recent.addRow(new Object[]{a.getCompanyName(), a.getPosition(), a.getStatus(), a.getAppliedDate(), a.getUpdatedAt()});
                    message.setText(s.total() == 0 ? "Chưa có đơn. Thêm công ty và vị trí, sau đó tạo đơn ứng tuyển." : "Dữ liệu đã cập nhật.");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { message.setText("Tải thống kê thất bại. Kiểm tra cấu hình MySQL rồi bấm Làm mới."); Ui.error(DashboardPanel.this, e.getCause()); }
            }
        }.execute();
    }
}

