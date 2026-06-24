package com.tbg.wms.v2.desktop;

import com.tbg.wms.v2.desktop.shell.DesktopShellViewModel;

/**
 * Owns the desktop shell view model used by the WMS 2.0 desktop adapter.
 */
public final class DesktopApp {
    private final DesktopShellViewModel shell;

    /**
     * Creates a desktop application wrapper.
     *
     * @param shell shell view model owned by the application
     */
    public DesktopApp(DesktopShellViewModel shell) {
        if (shell == null) {
            throw new IllegalArgumentException("shell is required");
        }
        this.shell = shell;
    }

    /**
     * Creates the standard desktop shell configuration.
     *
     * @return desktop application using the standard shell
     */
    public static DesktopApp standard() {
        return new DesktopApp(DesktopShellViewModel.standard());
    }

    /**
     * Returns the desktop shell view model.
     *
     * @return owned shell view model
     */
    public DesktopShellViewModel shell() {
        return shell;
    }
}
