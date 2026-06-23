package com.tbg.wms.v2.desktop;

import com.tbg.wms.v2.desktop.shell.DesktopShellViewModel;

public final class DesktopApp {
    private final DesktopShellViewModel shell;

    public DesktopApp(DesktopShellViewModel shell) {
        if (shell == null) {
            throw new IllegalArgumentException("shell is required");
        }
        this.shell = shell;
    }

    public static DesktopApp standard() {
        return new DesktopApp(DesktopShellViewModel.standard());
    }

    public DesktopShellViewModel shell() {
        return shell;
    }
}
