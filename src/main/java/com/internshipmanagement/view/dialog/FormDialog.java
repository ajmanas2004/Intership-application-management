package com.internshipmanagement.view.dialog;

import com.internshipmanagement.service.ValidationException;
import com.internshipmanagement.view.Ui;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public abstract class FormDialog extends JDialog {
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JButton save = new JButton("Lưu");
    private int row;
    private boolean saved;

    protected FormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        form.setBorder(new EmptyBorder(16, 16, 8, 16));
        add(form, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Hủy");
        cancel.addActionListener(e -> { if (save.isEnabled()) dispose(); });
        save.addActionListener(e -> {
            try {
                Runnable action = saveAction();
                setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                Ui.run(this, save, () -> {
                    try { action.run(); return true; }
                    finally { SwingUtilities.invokeLater(() -> setDefaultCloseOperation(DISPOSE_ON_CLOSE)); }
                }, result -> { saved = true; dispose(); });
            } catch (RuntimeException error) { Ui.error(this, error); }
        });
        footer.add(cancel); footer.add(save); add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> { if (save.isEnabled()) dispose(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setMinimumSize(new Dimension(510, 260));
    }

    protected void field(String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(6, 4, 6, 12);
        form.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, c); row++;
    }

    protected static JTextArea textArea(String value) {
        JTextArea a = new JTextArea(value == null ? "" : value, 5, 28);
        a.setLineWrap(true); a.setWrapStyleWord(true);
        return a;
    }

    protected static LocalDate date(JTextField field, String label) {
        if (field.getText().isBlank()) return null;
        try { return LocalDate.parse(field.getText().strip()); }
        catch (DateTimeParseException e) { throw new ValidationException(label + " phải là ngày hợp lệ, dạng yyyy-MM-dd."); }
    }

    protected static String showDate(LocalDate value) { return value == null ? "" : value.toString(); }
    protected abstract Runnable saveAction();

    public boolean showDialog() {
        pack(); setLocationRelativeTo(getOwner()); setVisible(true); return saved;
    }
}

