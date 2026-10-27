package com.internshipmanagement.view;

import com.internshipmanagement.entity.*;
import com.internshipmanagement.service.DashboardService;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.function.Consumer;
import java.awt.event.*;

public class DashboardPanel extends JPanel implements NavigableScreen {
    private final DashboardService service;
    private final JLabel total = number();
    private final JLabel submitted = number();
    private final Map<ApplicationStatus, JLabel> counts = new EnumMap<>(ApplicationStatus.class);
    private final DefaultTableModel recent = new DefaultTableModel(new String[]{"Công ty", "Vị trí", "Trạng thái", "Ngày ứng tuyển", "Cập nhật", "Hành động"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return col == 5 && !loading; }
    };
    private final JLabel message = new JLabel(" ");
    private final JTable table = new JTable(recent);
    private final JScrollPane scroll = new JScrollPane(table);
    private List<Application> applications = List.of();
    private Consumer<NavigationTarget> navigator = target -> {};
    private State pending;
    private State loadingState;
    private boolean loading;
    private record State(Long selectedId, List<RowSorter.SortKey> sorting, int scrollY) {
        State { sorting = List.copyOf(sorting); }
    }
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
        cards.add(linkCard("Tổng đơn theo dõi", total, null)); cards.add(card("Tổng đã ứng tuyển", submitted));
        for (ApplicationStatus status : ApplicationStatus.values()) {
            JLabel value = number(); counts.put(status, value); cards.add(linkCard(status.toString(), value, status));
        }
        cards.add(new JLabel("<html>Thêm công ty, vị trí và đơn<br>để theo dõi tiến độ ứng tuyển.</html>"));
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        JPanel list = new JPanel(new BorderLayout(8, 8));
        JPanel listHeading = new JPanel(new BorderLayout());
        listHeading.add(new JLabel("10 đơn cập nhật gần đây · Bấm đúp dòng để mở đơn"), BorderLayout.WEST);
        list.add(listHeading, BorderLayout.NORTH);
        table.setRowHeight(28); table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowActions.install(table,5,index -> applications.get(index),() -> !loading,
                List.of(new TableRowActions.Action<>("Mở đơn", a -> navigator.accept(NavigationTarget.application(a)))));
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0
                        && table.columnAtPoint(e.getPoint()) != 5) openSelected();
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-application");
        table.getActionMap().put("open-application", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openSelected(); }
        });
        list.add(scroll, BorderLayout.CENTER); add(list, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
    }
    @Override public void setNavigator(Consumer<NavigationTarget> action) { navigator = action; }
    @Override public String navigationTitle() { return "Tổng quan"; }
    private JPanel linkCard(String title, JLabel value, ApplicationStatus status) {
        JPanel panel = card(title + "  →", value);
        Runnable action = () -> navigator.accept(NavigationTarget.applications(status));
        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) action.run(); }
        };
        panel.setName(status == null ? "dashboard-all" : "dashboard-" + status.name());
        panel.setFocusable(true); panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.setToolTipText("Xem danh sách đơn: " + title); panel.addMouseListener(click);
        for (Component child : panel.getComponents()) { child.setCursor(panel.getCursor()); child.addMouseListener(click); }
        panel.registerKeyboardAction(e -> action.run(), KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(e -> action.run(), KeyStroke.getKeyStroke("SPACE"), JComponent.WHEN_FOCUSED);
        return panel;
    }
    private void openSelected() {
        if (!loading && table.getSelectedRow() >= 0)
            navigator.accept(NavigationTarget.application(applications.get(table.convertRowIndexToModel(table.getSelectedRow()))));
    }
    @Override public Object captureState() {
        if (loading && loadingState != null) return loadingState;
        Long id = table.getSelectedRow() < 0 ? null : applications.get(table.convertRowIndexToModel(table.getSelectedRow())).getId();
        return new State(id, table.getRowSorter().getSortKeys().stream().map(k -> new RowSorter.SortKey(k.getColumn(), k.getSortOrder())).toList(),
                scroll.getVerticalScrollBar().getValue());
    }
    @Override public void restoreState(Object state) { pending = (State) state; refresh(); }
    private static JLabel number() {
        JLabel label = new JLabel("—"); label.setFont(label.getFont().deriveFont(Font.BOLD, 30f)); return label;
    }
    private static JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(4, 12));
        panel.setBackground(Color.WHITE); panel.setBorder(new CompoundBorder(new LineBorder(new Color(220, 225, 230)), new EmptyBorder(12, 14, 12, 14)));
        panel.add(new JLabel(title), BorderLayout.NORTH); panel.add(value, BorderLayout.CENTER); return panel;
    }
    @Override public void refresh() {
        State state = pending == null ? (State) captureState() : pending;
        pending = null; loadingState = state; loading = true;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        table.setEnabled(false);
        long token = ++request; message.setText("Đang tải thống kê...");
        new SwingWorker<DashboardService.Snapshot, Void>() {
            @Override protected DashboardService.Snapshot doInBackground() { return service.snapshot(); }
            @Override protected void done() {
                if (token != request) return;
                try {
                    DashboardService.Snapshot s = get();
                    total.setText(Long.toString(s.total())); submitted.setText(Long.toString(s.submitted()));
                    counts.forEach((key, label) -> label.setText(s.counts().get(key).toString()));
                    table.clearSelection(); recent.getDataVector().clear();
                    applications = s.recent();
                    for (Application a : applications) recent.getDataVector().add(new Vector<>(Arrays.asList(a.getCompanyName(), a.getPosition(), a.getStatus(), a.getAppliedDate(),
                            a.getUpdatedAt() == null ? "" : a.getUpdatedAt().toLocalDate().toString(), "")));
                    recent.fireTableDataChanged();
                    loading = false; table.setEnabled(true); table.getRowSorter().setSortKeys(state.sorting());
                    for (int i = 0; i < applications.size(); i++) if (Objects.equals(state.selectedId(), applications.get(i).getId())) {
                        int view = table.convertRowIndexToView(i); table.setRowSelectionInterval(view, view); break;
                    }
                    SwingUtilities.invokeLater(() -> { if (token == request) scroll.getVerticalScrollBar().setValue(state.scrollY()); });
                    message.setText(s.total() == 0 ? "Chưa có đơn. Thêm công ty và vị trí, sau đó tạo đơn ứng tuyển." : "Dữ liệu đã cập nhật.");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) {
                    loading = false; applications = List.of(); recent.setRowCount(0); table.setEnabled(true);
                    message.setText("Tải thống kê thất bại. Kiểm tra cấu hình MySQL rồi bấm Làm mới."); Ui.error(DashboardPanel.this, e.getCause());
                }
            }
        }.execute();
    }
}

