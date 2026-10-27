package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Consumer;
import com.internshipmanagement.service.ValidationException;

public abstract class DataPanel<T> extends JPanel implements NavigableScreen {
    protected final JTextField search = new JTextField(22);
    protected final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    protected final JTable table;
    protected final JLabel status = new JLabel(" ");
    protected List<T> rows = List.of();
    protected boolean loading;
    protected boolean loadSuccessful;
    protected final JLabel context = new JLabel(" ");
    protected Consumer<NavigationTarget> navigator = target -> {};
    protected Runnable navigationChanged = () -> {};
    protected long viewVersion;
    private boolean active = true;
    private final String pageTitle;
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final DefaultTableModel model;
    private final Function<T, Object[]> cells;
    private final Function<T, Long> identity;
    private final JScrollPane scroll;
    private int actionColumn = -1;
    private TableState pendingRestore;
    private TableState loadingState;
    private long request;

    protected DataPanel(String title, String[] columns, Function<T, Object[]> cells, Function<T, Long> identity) {
        super(new BorderLayout(8, 8));
        this.cells = cells; this.identity = identity; this.pageTitle = title;
        setBorder(new EmptyBorder(12, 20, 16, 20));
        JPanel top = new JPanel(new BorderLayout(4, 8));
        JPanel heading = new JPanel(new BorderLayout(4, 4));
        heading.add(Ui.title(title), BorderLayout.NORTH); heading.add(context, BorderLayout.SOUTH);
        top.add(heading, BorderLayout.NORTH);
        toolbar.add(new JLabel("Tìm kiếm:")); toolbar.add(search);
        JPanel controls = new JPanel(new BorderLayout());
        controls.add(toolbar, BorderLayout.NORTH); controls.add(actions, BorderLayout.SOUTH);
        top.add(controls, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column == actionColumn && !loading && loadSuccessful;
            }
            @Override public Class<?> getColumnClass(int column) {
                if (column == actionColumn) return Object.class;
                for (int row = 0; row < getRowCount(); row++)
                    if (getValueAt(row, column) != null) return getValueAt(row, column).getClass();
                return Object.class;
            }
        };
        table = new JTable(model) {
            @Override public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
                if (row < 0 || column < 0) return null;
                if (convertColumnIndexToModel(column) == actionColumn) return "Thao tác cho dòng này";
                Object value = getValueAt(row,column);
                return value == null ? null : value.toString();
            }
        };
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        search.addActionListener(e -> refresh());
    }

    @Override public void setNavigator(Consumer<NavigationTarget> action) { navigator = action; }
    @Override public void setNavigationListener(Runnable action) { navigationChanged = action; }
    @Override public void setPageActive(boolean value) { active = value; viewVersion++; }
    protected boolean isCurrentView(long version) { return active && viewVersion == version; }
    @Override public String navigationTitle() { return pageTitle; }
    protected String emptyMessage() { return "Không có dữ liệu phù hợp. Thử Xóa bộ lọc hoặc Thêm."; }

    protected record TableState(String search, Long selectedId, List<RowSorter.SortKey> sorting, int scrollY) {
        public TableState { sorting = List.copyOf(sorting); }
    }
    protected TableState captureTableState() {
        if (loading && loadingState != null)
            return new TableState(search.getText(), loadingState.selectedId(), loadingState.sorting(), loadingState.scrollY());
        Long id = table.getSelectedRow() < 0 ? null : identity.apply(rows.get(table.convertRowIndexToModel(table.getSelectedRow())));
        List<RowSorter.SortKey> sorting = table.getRowSorter().getSortKeys().stream()
                .map(k -> new RowSorter.SortKey(k.getColumn(), k.getSortOrder())).toList();
        return new TableState(search.getText(), id, sorting, scroll.getVerticalScrollBar().getValue());
    }
    protected void restoreTableState(TableState state) { search.setText(state.search()); pendingRestore = state; }
    protected void resetTableState() { restoreTableState(new TableState("", null, List.of(), 0)); }
    protected void focusRow(Long id) {
        TableState state = pendingRestore == null ? captureTableState() : pendingRestore;
        pendingRestore = new TableState(state.search(), id, state.sorting(), 0);
    }
    @Override public Object captureState() { return captureTableState(); }
    @Override public void restoreState(Object state) { restoreTableState((TableState) state); refresh(); }
    @Override public void openPage(NavigationTarget target) { resetTableState(); refresh(); }

    protected TableRowActions.Action<T> action(String label, Consumer<T> callback) {
        return new TableRowActions.Action<>(label, callback);
    }
    @SafeVarargs
    protected final void rowActions(TableRowActions.Action<T>... actions) {
        actionColumn = model.getColumnCount();
        model.addColumn("Hành động");
        TableRowActions.install(table, actionColumn, index -> rows.get(index), () -> !loading && loadSuccessful, List.of(actions));
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
    }

    /** A link column supports one click; every row also supports double-click or Enter. */
    protected void activateRows(int linkColumn, Consumer<T> action) {
        table.setToolTipText("Bấm tên để mở, hoặc chọn dòng rồi nhấn Enter.");
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint()), column = table.columnAtPoint(e.getPoint());
                if (!SwingUtilities.isLeftMouseButton(e) || row < 0 || column < 0 || loading || !loadSuccessful) return;
                if (table.convertColumnIndexToModel(column) == actionColumn) return;
                if ((e.getClickCount() == 1 && table.convertColumnIndexToModel(column) == linkColumn)
                        || (e.getClickCount() == 2 && table.convertColumnIndexToModel(column) != linkColumn)) {
                    table.setRowSelectionInterval(row, row);
                    action.accept(selected());
                }
            }
        });
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                boolean link = !loading && column >= 0 && table.rowAtPoint(e.getPoint()) >= 0
                        && table.convertColumnIndexToModel(column) == linkColumn;
                table.setCursor(Cursor.getPredefinedCursor(link ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-selected");
        table.getActionMap().put("open-selected", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!loading && loadSuccessful && table.getSelectedRow() >= 0) {
                    if (table.getSelectedColumn() >= 0 && table.convertColumnIndexToModel(table.getSelectedColumn()) == actionColumn) {
                        table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
                        if (table.getEditorComponent() != null) table.getEditorComponent().transferFocus();
                    } else action.accept(selected());
                }
            }
        });
        table.getColumnModel().getColumn(linkColumn).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                if (!selected) c.setForeground(new Color(30, 85, 160));
                return c;
            }
        });
    }

    protected JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            try { action.run(); } catch (RuntimeException error) { Ui.error(this, error); }
        });
        actions.add(b);
        return b;
    }

    protected T selected() {
        if (loading || !loadSuccessful) throw new ValidationException("Hãy đợi danh sách tải thành công.");
        int row = table.getSelectedRow();
        if (row < 0) throw new ValidationException("Hãy chọn một dòng trong danh sách.");
        return rows.get(table.convertRowIndexToModel(row));
    }

    protected void load(Callable<List<T>> query) {
        long token = ++request;
        TableState state = pendingRestore == null ? captureTableState() : pendingRestore;
        pendingRestore = null; loadingState = state;
        loading = true; loadSuccessful = false;
        navigationChanged.run();
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        table.clearSelection(); table.setEnabled(false);
        status.setText("Đang tải...");
        new SwingWorker<List<T>, Void>() {
            @Override protected List<T> doInBackground() throws Exception { return query.call(); }
            @Override protected void done() {
                if (token != request) return;
                loading = false; table.setEnabled(true);
                try {
                    rows = List.copyOf(get());
                    replaceTableRows();
                    loadSuccessful = true;
                    table.getRowSorter().setSortKeys(state.sorting());
                    for (int index = 0; index < rows.size(); index++)
                        if (java.util.Objects.equals(state.selectedId(), identity.apply(rows.get(index)))) {
                            int view = table.convertRowIndexToView(index);
                            table.setRowSelectionInterval(view, view); break;
                        }
                    SwingUtilities.invokeLater(() -> {
                        if (token == request) scroll.getVerticalScrollBar().setValue(state.scrollY());
                    });
                    status.setText(rows.isEmpty() ? emptyMessage() : rows.size() + " bản ghi");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) {
                    rows = List.of(); replaceTableRows();
                    status.setText("Tải dữ liệu thất bại. Hãy thử Làm mới."); Ui.error(DataPanel.this, e.getCause());
                }
            }
        }.execute();
    }

    private void replaceTableRows() {
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        table.clearSelection();
        model.getDataVector().clear();
        for (T value : rows) {
            Object[] data = cells.apply(value);
            if (actionColumn >= 0) data = java.util.Arrays.copyOf(data, data.length + 1);
            model.getDataVector().add(new java.util.Vector<>(java.util.Arrays.asList(data)));
        }
        model.fireTableDataChanged();
    }

    protected void delete(String description, Runnable action) {
        if (JOptionPane.showConfirmDialog(this, "Xóa " + description + "?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            Ui.run(this, null, () -> { action.run(); return null; }, result -> refresh());
    }
}

