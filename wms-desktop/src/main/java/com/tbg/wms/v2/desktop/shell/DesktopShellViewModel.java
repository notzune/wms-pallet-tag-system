package com.tbg.wms.v2.desktop.shell;

import java.util.List;

public final class DesktopShellViewModel {
    private final List<DesktopScreen> screens;
    private DesktopScreen currentScreen;

    public DesktopShellViewModel(List<DesktopScreen> screens) {
        if (screens == null || screens.isEmpty()) {
            throw new IllegalArgumentException("screens are required");
        }
        this.screens = List.copyOf(screens);
        this.currentScreen = this.screens.get(0);
    }

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

    public List<DesktopScreen> screens() {
        return screens;
    }

    public DesktopScreen currentScreen() {
        return currentScreen;
    }

    public void select(String screenId) {
        currentScreen = screens.stream()
                .filter(screen -> screen.id().equals(screenId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screen: " + screenId));
    }
}
