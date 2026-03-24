/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.PrintService;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
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
    private final PdfDocumentLoader pdfDocumentLoader;
    private final PrinterJobFactory printerJobFactory;

    public RailPrintService() {
        this(path -> PDDocument.load(path.toFile()), PrinterJob::getPrinterJob);
    }

    RailPrintService(PdfDocumentLoader pdfDocumentLoader, PrinterJobFactory printerJobFactory) {
        this.pdfDocumentLoader = Objects.requireNonNull(pdfDocumentLoader, "pdfDocumentLoader cannot be null");
        this.printerJobFactory = Objects.requireNonNull(printerJobFactory, "printerJobFactory cannot be null");
    }

    /**
     * Sends a rendered rail PDF to the host default printer without depending on a shell file association.
     *
     * @param documentPath rendered PDF path
     */
    public void print(Path documentPath) throws IOException {
        Objects.requireNonNull(documentPath, "documentPath cannot be null");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document file does not exist: " + documentPath);
        }
        printPdfToDefaultPrinter(documentPath);
    }

    /**
     * Validates that the host exposes a usable system default printer for rail PDF printing
     * without submitting a live print job.
     */
    public String validateSystemDefaultPrinter() throws IOException {
        try {
            PrinterJob printerJob = printerJobFactory.create();
            PrintService printService = printerJob.getPrintService();
            if (printService == null) {
                throw new IOException("No system default printer is configured on this host.");
            }
            return "System default printer: " + printService.getName();
        } catch (HeadlessException e) {
            throw new IOException("System-default PDF printing is not available on a headless host.", e);
        }
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
        PrinterRoutingService routing = PrinterRoutingService.load(
                site,
                RuntimePathResolver.resolveWorkingDirOrJarSiblingDir(RailPrintService.class, "config")
        );
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

    private void printPdfToDefaultPrinter(Path documentPath) throws IOException {
        try (PDDocument document = pdfDocumentLoader.load(documentPath)) {
            PrinterJob printerJob = requireSystemDefaultPrinterJob();
            printerJob.setPageable(new PDFPageable(document));
            printerJob.print();
        } catch (HeadlessException e) {
            throw new IOException("System-default PDF printing is not available on a headless host.", e);
        } catch (PrinterException e) {
            throw new IOException("System-default PDF printing failed: " + e.getMessage(), e);
        }
    }

    private PrinterJob requireSystemDefaultPrinterJob() throws IOException {
        PrinterJob printerJob = printerJobFactory.create();
        PrintService printService = printerJob.getPrintService();
        if (printService == null) {
            throw new IOException("No system default printer is configured on this host.");
        }
        return printerJob;
    }

    @FunctionalInterface
    interface PdfDocumentLoader {
        PDDocument load(Path documentPath) throws IOException;
    }

    @FunctionalInterface
    interface PrinterJobFactory {
        PrinterJob create();
    }
}
