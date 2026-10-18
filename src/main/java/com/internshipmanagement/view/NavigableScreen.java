package com.internshipmanagement.view;

import java.util.function.Consumer;

public interface NavigableScreen extends Refreshable {
    default Object captureState() { return null; }
    default void restoreState(Object state) { refresh(); }
    default void openPage(NavigationTarget target) { refresh(); }
    default String navigationTitle() { return ""; }
    default void setNavigator(Consumer<NavigationTarget> navigator) {}
    default void setNavigationListener(Runnable listener) {}
    default void setPageActive(boolean active) {}
}
