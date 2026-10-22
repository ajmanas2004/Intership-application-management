package com.internshipmanagement.view;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
public final class Ui {
    private Ui() {}
    public static void error(Component owner, Throwable error) {
        JOptionPane.showMessageDialog(owner, error.getMessage() == null ? "Thao tác thất bại." : error.getMessage(),
                "Không thể thực hiện", JOptionPane.ERROR_MESSAGE);
    }
    /** JDBC and file work run off EDT; callbacks run on EDT. */
    public static <T> void run(Component owner, AbstractButton trigger, Callable<T> work, Consumer<T> done) {
        if (trigger != null) trigger.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                if (trigger != null) trigger.setEnabled(true);
                try { done.accept(get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { error(owner, e.getCause()); }
                catch (RuntimeException e) { error(owner, e); }
            }
        }.execute();
    }
    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 24f));
        return l;
    }
}

