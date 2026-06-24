package com.tbg.wms.v2.desktop.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DesktopShellViewModelTest {
    @Test
    void shellRegistersOnlySupported2WorkflowScreens() {
        DesktopShellViewModel shell = DesktopShellViewModel.standard();

        assertEquals(
                java.util.List.of("labels", "queue", "resume", "barcode", "rail", "zpl-preview", "settings"),
                shell.screens().stream().map(DesktopScreen::id).toList()
        );
    }

    @Test
    void selectingScreenUpdatesCurrentScreen() {
        DesktopShellViewModel shell = DesktopShellViewModel.standard();

        shell.select("barcode");

        assertEquals("barcode", shell.currentScreen().id());
        assertEquals("Barcode", shell.currentScreen().title());
    }
}
