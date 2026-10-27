package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.*;
import java.util.Objects;

/** A nullable LocalDate field with a calendar; dates never require typing. */
public class DatePicker extends JPanel {
    private final JTextField display = new JTextField(10);
    private final JButton choose = new JButton("Chọn ngày");
    private final JButton clear = new JButton("×");
    private final JPopupMenu popup = new JPopupMenu();
    private final JPanel days = new JPanel(new GridLayout(7, 7, 2, 2));
    private final JSpinner month = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
    private final JSpinner year = new JSpinner(new SpinnerNumberModel(2026, 1000, 9999, 1));
    private final JButton previous = new JButton("‹"), next = new JButton("›"), today = new JButton("Hôm nay");
    private LocalDate value;
    private LocalDate maximum;
    private YearMonth shown;
    private boolean updating;

    public DatePicker(LocalDate initial) {
        super(new BorderLayout(4, 0));
        display.setEditable(false); display.setToolTipText("Chọn ngày từ lịch, định dạng yyyy-MM-dd");
        choose.setToolTipText("Mở lịch chọn ngày"); clear.setToolTipText("Xóa ngày");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttons.add(choose); buttons.add(clear); add(display, BorderLayout.CENTER); add(buttons, BorderLayout.EAST);
        JPanel calendar = new JPanel(new BorderLayout(4, 8));
        calendar.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        month.setEditor(new JSpinner.NumberEditor(month, "0")); year.setEditor(new JSpinner.NumberEditor(year, "0"));
        header.add(previous); header.add(new JLabel("Tháng")); header.add(month);
        header.add(new JLabel("Năm")); header.add(year); header.add(next);
        calendar.add(header, BorderLayout.NORTH); calendar.add(days, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton empty = new JButton("Để trống");
        footer.add(today); footer.add(empty); calendar.add(footer, BorderLayout.SOUTH);
        popup.add(calendar);
        previous.addActionListener(e -> moveMonth(-1)); next.addActionListener(e -> moveMonth(1));
        month.addChangeListener(e -> changeMonth()); year.addChangeListener(e -> changeMonth());
        today.addActionListener(e -> pick(LocalDate.now()));
        empty.addActionListener(e -> pick(null)); clear.addActionListener(e -> setDate(null));
        choose.addActionListener(e -> {
            shown = YearMonth.from(value == null ? LocalDate.now() : value);
            renderMonth(); popup.show(this, 0, getHeight());
        });
        display.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "calendar");
        display.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "calendar");
        display.getActionMap().put("calendar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (isEnabled()) choose.doClick(); }
        });
        shown = YearMonth.from(initial == null ? LocalDate.now() : initial);
        setDate(initial);
    }

    public LocalDate getDate() { return value; }
    public void setDate(LocalDate date) {
        if (date != null && (date.getYear() < 1000 || date.getYear() > 9999 || maximum != null && date.isAfter(maximum)))
            throw new IllegalArgumentException("Ngày được chọn nằm ngoài khoảng cho phép.");
        LocalDate old = value; value = date;
        display.setText(date == null ? "" : date.toString());
        if (date != null) shown = YearMonth.from(date);
        renderMonth();
        if (!Objects.equals(old, date)) firePropertyChange("date", old, date);
    }
    public void setMaximumDate(LocalDate date) {
        maximum = date;
        if (value != null && date != null && value.isAfter(date)) setDate(null); else renderMonth();
    }
    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (display != null) { display.setEnabled(enabled); choose.setEnabled(enabled); clear.setEnabled(enabled); }
        if (!enabled && popup != null) popup.setVisible(false);
    }
    private void pick(LocalDate date) { setDate(date); popup.setVisible(false); }
    private void changeMonth() {
        if (!updating) { shown = YearMonth.of((Integer) year.getValue(), (Integer) month.getValue()); renderMonth(); }
    }
    private void moveMonth(int amount) {
        YearMonth candidate = shown.plusMonths(amount);
        if (candidate.getYear() >= 1000 && candidate.getYear() <= 9999) { shown = candidate; renderMonth(); }
    }
    private void renderMonth() {
        updating = true;
        try { month.setValue(shown.getMonthValue()); year.setValue(shown.getYear()); }
        finally { updating = false; }
        previous.setEnabled(shown.isAfter(YearMonth.of(1000, 1)));
        next.setEnabled(shown.isBefore(YearMonth.of(9999, 12)));
        days.removeAll();
        for (String label : new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"}) days.add(new JLabel(label, SwingConstants.CENTER));
        int offset = shown.atDay(1).getDayOfWeek().getValue() - 1;
        for (int cell = 0; cell < 42; cell++) {
            int day = cell - offset + 1;
            if (day < 1 || day > shown.lengthOfMonth()) { days.add(new JLabel("")); continue; }
            LocalDate date = shown.atDay(day);
            JButton button = new JButton(Integer.toString(day));
            button.setName("calendar-day-" + date); button.setToolTipText(date.toString());
            button.setMargin(new Insets(3, 5, 3, 5));
            button.setEnabled(maximum == null || !date.isAfter(maximum));
            if (date.equals(value)) {
                button.setBackground(new Color(42, 92, 155)); button.setForeground(Color.WHITE);
            }
            if (date.equals(LocalDate.now())) button.setFont(button.getFont().deriveFont(Font.BOLD));
            button.addActionListener(e -> pick(date)); days.add(button);
        }
        today.setEnabled(maximum == null || !LocalDate.now().isAfter(maximum));
        days.revalidate(); days.repaint();
        if (popup.isVisible()) popup.pack();
    }
    JPopupMenu calendar() { return popup; }
}
