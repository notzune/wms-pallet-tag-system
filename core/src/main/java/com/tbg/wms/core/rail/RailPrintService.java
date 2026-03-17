/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Sends generated card documents to the host print shell.
 */
public final class RailPrintService {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int WRITE_TIMEOUT_MS = 10000;

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

    /**
     * Sends a rendered rail PDF to the configured rail printer first, then falls back to system print dialog.
     *
     * @param documentPath rendered PDF path
     * @param config       application config
     */
    public void print(Path documentPath, AppConfig config) throws IOException {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(documentPath, "documentPath cannot be null");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document file does not exist: " + documentPath);
        }

        PrinterConfig printer = resolveRailPrinter(config);
        try {
            sendFileRaw(printer, documentPath);
            return;
        } catch (IOException directPrintError) {
            printWithSystemDialog(documentPath, directPrintError);
        }
    }

    /**
     * Sends a rendered rail PDF to the specified printer first, then falls back to system print dialog.
     *
     * @param documentPath rendered PDF path
     * @param printer      explicit target printer
     */
    public void print(Path documentPath, PrinterConfig printer) throws IOException {
        Objects.requireNonNull(printer, "printer cannot be null");
        Objects.requireNonNull(documentPath, "documentPath cannot be null");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document file does not exist: " + documentPath);
        }
        try {
            sendFileRaw(printer, documentPath);
            return;
        } catch (IOException directPrintError) {
            printWithSystemDialog(documentPath, directPrintError);
        }
    }

    private PrinterConfig resolveRailPrinter(AppConfig config) throws IOException {
        String site = config.activeSiteCode();
        PrinterRoutingService routing = PrinterRoutingService.load(site, Path.of("config"));
        String printerId = config.railDefaultPrinterIdOrNull();
        if (printerId == null || printerId.isBlank()) {
            printerId = routing.getDefaultPrinterId();
        }
        final String resolvedPrinterId = printerId;
        return routing.findPrinter(printerId)
                .orElseThrow(() -> new IOException("Configured rail printer is missing/disabled: " + resolvedPrinterId));
    }

    private void sendFileRaw(PrinterConfig printer, Path documentPath) throws IOException {
        InetSocketAddress address = new InetSocketAddress(printer.getIp(), printer.getPort());
        try (Socket socket = new Socket()) {
            socket.connect(address, CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(WRITE_TIMEOUT_MS);
            try (OutputStream out = socket.getOutputStream()) {
                Files.copy(documentPath, out);
                out.flush();
            }
        }
    }

    private void printWithSystemDialog(Path documentPath, IOException originalFailure) throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            IOException ex = new IOException("Direct rail printer send failed and host is headless; cannot show print dialog.");
            ex.addSuppressed(originalFailure);
            throw ex;
        }

        try (PDDocument document = PDDocument.load(documentPath.toFile())) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPageable(new PDFPageable(document));
            boolean accepted = printerJob.printDialog();
            if (!accepted) {
                IOException ex = new IOException("Direct rail printer send failed and print dialog was canceled.");
                ex.addSuppressed(originalFailure);
                throw ex;
            }
            printerJob.print();
        } catch (PrinterException e) {
            IOException ex = new IOException("Direct rail printer send failed and dialog print failed: " + e.getMessage(), e);
            ex.addSuppressed(originalFailure);
            throw ex;
        }
    }
}
