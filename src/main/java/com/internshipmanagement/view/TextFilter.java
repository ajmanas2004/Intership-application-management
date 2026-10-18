package com.internshipmanagement.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Empty value is the "all" option; real data may have the same text as its label. */
public class TextFilter extends JComboBox<String> {
    private boolean updating;
    private Runnable changed = () -> {};
    public TextFilter(String allLabel) {
        addItem(""); setPrototypeDisplayValue(allLabel);
        setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null || value.toString().isEmpty() ? allLabel : value, index, selected, focus);
            }
        });
        addActionListener(e -> { if (!updating) changed.run(); });
    }
    public void onChange(Runnable action) { changed = action; }
    public String value() { return getSelectedItem() instanceof String s && !s.isEmpty() ? s : null; }
    public void select(String value) {
        updating = true;
        try {
            String desired = value == null ? "" : value;
            boolean found = false;
            for (int i = 0; i < getItemCount(); i++) if (desired.equals(getItemAt(i))) found = true;
            if (!found) addItem(desired);
            setSelectedItem(desired);
        } finally { updating = false; }
    }
    public boolean updateOptions(List<String> options) {
        String value = value();
        List<String> clean = options.stream().filter(s -> s != null && !s.isBlank()).distinct().sorted().toList();
        boolean missing = value != null && !clean.contains(value);
        updating = true;
        try {
            removeAllItems(); addItem("");
            clean.forEach(this::addItem); setSelectedItem(missing || value == null ? "" : value);
        } finally { updating = false; }
        return missing;
    }
}
