package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import com.internshipmanagement.service.ValidationException;

public abstract class DataPanel<T> extends JPanel implements Refreshable {
    protected final JTextField search = new JTextField(22);
    protected final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    protected final JTable table;
    protected final JLabel status = new JLabel(" ");
    protected List<T> rows = List.of();
    private final DefaultTableModel model;
    private final Function<T, Object[]> cells;
    private long request;

    protected DataPanel(String title, String[] columns, Function<T, Object[]> cells) {
        super(new BorderLayout(8, 8));
        this.cells = cells;
        setBorder(new EmptyBorder(12, 20, 16, 20));
        JPanel top = new JPanel(new BorderLayout(4, 8));
        top.add(Ui.title(title), BorderLayout.NORTH);
        toolbar.add(new JLabel("Tìm kiếm:")); toolbar.add(search);
        top.add(toolbar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        search.addActionListener(e -> refresh());
    }

    protected JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            try { action.run(); } catch (RuntimeException error) { Ui.error(this, error); }
        });
        toolbar.add(b);
        return b;
    }

    protected T selected() {
        int row = table.getSelectedRow();
        if (row < 0) throw new ValidationException("Hãy chọn một dòng trong danh sách.");
        return rows.get(table.convertRowIndexToModel(row));
    }

    protected void load(Callable<List<T>> query) {
        long token = ++request;
        status.setText("Đang tải...");
        new SwingWorker<List<T>, Void>() {
            @Override protected List<T> doInBackground() throws Exception { return query.call(); }
            @Override protected void done() {
                if (token != request) return;
                try {
                    rows = List.copyOf(get());
                    model.setRowCount(0);
                    for (T value : rows) model.addRow(cells.apply(value));
                    status.setText(rows.isEmpty() ? "Không có dữ liệu phù hợp." : rows.size() + " bản ghi");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { status.setText("Tải dữ liệu thất bại. Hãy thử Làm mới."); Ui.error(DataPanel.this, e.getCause()); }
            }
        }.execute();
    }

    protected void delete(String description, Runnable action) {
        if (JOptionPane.showConfirmDialog(this, "Xóa " + description + "?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            Ui.run(this, null, () -> { action.run(); return null; }, result -> refresh());
    }
}

