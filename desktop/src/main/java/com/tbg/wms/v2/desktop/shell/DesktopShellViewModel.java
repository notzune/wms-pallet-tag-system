package com.tbg.wms.v2.desktop.shell;

import java.util.List;

/**
 * Tracks desktop navigation screens and the currently selected screen.
 */
public final class DesktopShellViewModel {
    private final List<DesktopScreen> screens;
    private DesktopScreen currentScreen;

    /**
     * Creates a desktop shell view model.
     *
     * @param screens ordered screens available in the shell
     */
    public DesktopShellViewModel(List<DesktopScreen> screens) {
        if (screens == null || screens.isEmpty()) {
            throw new IllegalArgumentException("screens are required");
        }
        this.screens = List.copyOf(screens);
        this.currentScreen = this.screens.get(0);
    }

    /**
     * Creates the standard desktop shell configuration.
     *
     * @return standard desktop shell configuration
     */
    public static DesktopShellViewModel standard() {
        return new DesktopShellViewModel(List.of(
                new DesktopScreen("labels", "Label Print"),
                new DesktopScreen("queue", "Queue"),
                new DesktopScreen("resume", "Resume"),
                new DesktopScreen("barcode", "Barcode"),
                new DesktopScreen("rail", "Rail"),
                new DesktopScreen("zpl-preview", "ZPL Preview"),
                new DesktopScreen("settings", "Settings")
        ));
    }

    /**
     * Returns the ordered shell screens.
     *
     * @return immutable list of available screens
     */
    public List<DesktopScreen> screens() {
        return screens;
    }

    /**
     * Returns the currently selected shell screen.
     *
     * @return selected screen
     */
    public DesktopScreen currentScreen() {
        return currentScreen;
    }

    /**
     * Selects a shell screen by identifier.
     *
     * @param screenId screen identifier to select
     */
    public void select(String screenId) {
        currentScreen = screens.stream()
                .filter(screen -> screen.id().equals(screenId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screen: " + screenId));
    }
}
