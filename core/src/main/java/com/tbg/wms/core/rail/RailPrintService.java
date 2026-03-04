/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Sends generated card documents to the host print shell.
 */
public final class RailPrintService {

    /**
     * Sends a rendered document to the host printer using Desktop integration.
     *
     * @param documentPath rendered document path (PDF or DOCX)
     */
    public void print(Path documentPath) throws IOException {
        Objects.requireNonNull(documentPath, "documentPath cannot be null");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document file does not exist: " + documentPath);
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop print integration is not supported on this host.");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.PRINT)) {
            throw new IOException("Desktop PRINT action is not supported on this host.");
        }
        desktop.print(documentPath.toFile());
    }
}
