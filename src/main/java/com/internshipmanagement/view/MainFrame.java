package com.internshipmanagement.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Owns navigation only; screens own their query/filter state and services own data. */
public class MainFrame extends JFrame {
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
    private final Map<String, JPanel> screens = new LinkedHashMap<>();
    private final Map<String, JToggleButton> links = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    private final ButtonGroup navigationGroup = new ButtonGroup();
    private final Deque<Visit> back = new ArrayDeque<>();
    private final Deque<Visit> forward = new ArrayDeque<>();
    private final JButton backButton = new JButton("← Quay lại");
    private final JButton forwardButton = new JButton("Tiến tới →");
    private final JLabel location = new JLabel(" ");
    private NavigationTarget current;
    private record Visit(NavigationTarget target, Object state) {}

    public MainFrame() {
        super("Internship Application Management");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700)); setSize(1200, 800);
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(12, 16, 4, 16));
        header.add(Ui.title("Quản lý ứng tuyển thực tập"), BorderLayout.NORTH);
        header.add(navigation, BorderLayout.CENTER);
        JPanel history = new JPanel(new BorderLayout(14, 0));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        backButton.setToolTipText("Quay lại màn trước (Alt + ←)");
        forwardButton.setToolTipText("Tiến tới màn sau (Alt + →)");
        backButton.addActionListener(e -> goBack()); forwardButton.addActionListener(e -> goForward());
        buttons.add(backButton); buttons.add(forwardButton);
        history.add(buttons, BorderLayout.WEST); history.add(location, BorderLayout.CENTER);
        header.add(history, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH); add(cards, BorderLayout.CENTER);
        getRootPane().registerKeyboardAction(e -> goBack(), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> goForward(), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        updateNavigation(); setLocationRelativeTo(null);
    }

    public void addScreen(String key, String label, JPanel panel) {
        cards.add(panel, key); screens.put(key, panel); labels.put(key, label);
        if (panel instanceof NavigableScreen screen) {
            screen.setNavigator(this::navigate);
            screen.setNavigationListener(this::updateNavigation);
        }
        JToggleButton button = new JToggleButton(label);
        navigationGroup.add(button); links.put(key, button);
        button.addActionListener(e -> showScreen(key)); navigation.add(button);
    }

    public void showScreen(String key) { navigate(NavigationTarget.screen(key)); }

    public void navigate(NavigationTarget target) {
        if (!screens.containsKey(target.screen())) throw new IllegalArgumentException("Unknown screen: " + target.screen());
        if (target.equals(current)) {
            if (target.openForm() && screens.get(current.screen()) instanceof NavigableScreen screen) screen.openPage(target);
            else if (screens.get(current.screen()) instanceof Refreshable screen) screen.refresh();
            return;
        }
        if (current != null) back.push(capture());
        forward.clear(); current = target;
        display();
        if (screens.get(target.screen()) instanceof NavigableScreen screen) screen.openPage(target);
        else if (screens.get(target.screen()) instanceof Refreshable screen) screen.refresh();
        updateNavigation();
    }

    public void goBack() {
        if (back.isEmpty()) return;
        forward.push(capture()); restore(back.pop());
    }
    public void goForward() {
        if (forward.isEmpty()) return;
        back.push(capture()); restore(forward.pop());
    }
    private Visit capture() {
        JPanel panel = screens.get(current.screen());
        return new Visit(current, panel instanceof NavigableScreen screen ? screen.captureState() : null);
    }
    private void restore(Visit visit) {
        current = visit.target(); display();
        if (screens.get(current.screen()) instanceof NavigableScreen screen) screen.restoreState(visit.state());
        else if (screens.get(current.screen()) instanceof Refreshable screen) screen.refresh();
        updateNavigation();
    }
    private void display() {
        screens.forEach((key, panel) -> {
            if (panel instanceof NavigableScreen screen) screen.setPageActive(key.equals(current.screen()));
        });
        layout.show(cards, current.screen()); links.get(current.screen()).setSelected(true);
    }
    private void updateNavigation() {
        backButton.setEnabled(!back.isEmpty()); forwardButton.setEnabled(!forward.isEmpty());
        if (current == null) return;
        JPanel panel = screens.get(current.screen());
        String title = panel instanceof NavigableScreen screen ? screen.navigationTitle() : "";
        if (title.isBlank()) title = labels.get(current.screen());
        location.setText(title); location.setToolTipText(title);
    }
}

