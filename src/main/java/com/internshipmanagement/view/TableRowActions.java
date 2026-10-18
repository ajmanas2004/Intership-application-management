package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/** Render/edit one action cell; capture its model row before sorting or async work changes it. */
public final class TableRowActions {
    private TableRowActions() {}
    public record Action<T>(String label, Consumer<T> execute) {}

    public static <T> void install(JTable table, int modelColumn, IntFunction<T> row,
                                   BooleanSupplier available, List<Action<T>> actions) {
        Cell<T> cell = new Cell<>(table, row, available, List.copyOf(actions));
        TableColumn column = table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn));
        column.setCellRenderer(cell); column.setCellEditor(cell);
        table.putClientProperty("terminateEditOnFocusLost", true);
        JPanel sample = cell.panel(null, false, true);
        int width = sample.getPreferredSize().width + 10;
        column.setMinWidth(width); column.setPreferredWidth(width); column.setMaxWidth(width);
        table.setRowHeight(Math.max(table.getRowHeight(), sample.getPreferredSize().height + 6));
        if (table.getRowSorter() instanceof DefaultRowSorter<?, ?> sorter) sorter.setSortable(modelColumn, false);
    }

    private static final class Cell<T> extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private final JTable table;
        private final IntFunction<T> row;
        private final BooleanSupplier available;
        private final List<Action<T>> actions;

        Cell(JTable table, IntFunction<T> row, BooleanSupplier available, List<Action<T>> actions) {
            this.table = table; this.row = row; this.available = available; this.actions = actions;
        }
        JPanel panel(T value, boolean selected, boolean preview) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 1));
            panel.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            for (Action<T> action : actions) {
                JButton button = new JButton(action.label());
                button.setFont(table.getFont().deriveFont(11f)); button.setMargin(new Insets(2, 5, 2, 5));
                button.setToolTipText(action.label() + " cho dòng này");
                button.setEnabled(table.isEnabled() && available.getAsBoolean());
                if (!preview) button.addActionListener(e -> {
                    fireEditingCanceled();
                    SwingUtilities.invokeLater(() -> {
                        try { action.execute().accept(value); } catch (RuntimeException error) { Ui.error(table, error); }
                    });
                });
                panel.add(button);
            }
            return panel;
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int viewRow, int viewColumn) {
            return panel(null, selected, true);
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int viewRow, int viewColumn) {
            t.setRowSelectionInterval(viewRow, viewRow);
            return panel(row.apply(t.convertRowIndexToModel(viewRow)), selected, false);
        }
        @Override public Object getCellEditorValue() { return ""; }
        @Override public boolean isCellEditable(EventObject e) {
            return table.isEnabled() && available.getAsBoolean()
                    && (!(e instanceof MouseEvent mouse) || SwingUtilities.isLeftMouseButton(mouse));
        }
    }
}
