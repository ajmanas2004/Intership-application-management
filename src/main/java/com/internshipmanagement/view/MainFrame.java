package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
public class MainFrame extends JFrame {
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
    public MainFrame() {
        super("Internship Application Management");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650)); setSize(1200, 760);
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(12, 16, 8, 16));
        header.add(Ui.title("Quản lý ứng tuyển thực tập"), BorderLayout.NORTH);
        header.add(navigation, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH); add(cards, BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }
    public void addScreen(String key, String label, JPanel panel) {
        cards.add(panel, key);
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            layout.show(cards, key);
            if (panel instanceof Refreshable screen) screen.refresh();
        });
        navigation.add(button);
    }
    public void showScreen(String key) { layout.show(cards, key); }
}

