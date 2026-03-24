package com.tbg.wms.core.rail;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailPrintServiceTest {

    @Test
    void printUsesPdfPrinterJobForSystemDefaultWorkflow() throws Exception {
        Path pdf = Files.createTempFile("rail-system-default", ".pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }

        RecordingPrinterJob printerJob = new RecordingPrinterJob();
        RailPrintService service = new RailPrintService(
                path -> PDDocument.load(path.toFile()),
                () -> printerJob
        );

        service.print(pdf);

        assertTrue(printerJob.printCalled, "system-default rail printing should invoke PrinterJob#print");
        assertNotNull(printerJob.pageable, "system-default rail printing should attach the PDF as pageable content");
    }

    @Test
    void printWrapsPrinterFailuresWithActionableMessage() throws Exception {
        Path pdf = Files.createTempFile("rail-system-default-failure", ".pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }

        RailPrintService service = new RailPrintService(
                path -> PDDocument.load(path.toFile()),
                ThrowingPrinterJob::new
        );

        java.io.IOException error = assertThrows(java.io.IOException.class, () -> service.print(pdf));
        assertTrue(error.getMessage().contains("System-default PDF printing failed"));
    }

    @Test
    void printFailsClearlyWhenNoSystemDefaultPrinterIsConfigured() throws Exception {
        Path pdf = Files.createTempFile("rail-system-default-no-printer", ".pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }

        RailPrintService service = new RailPrintService(
                path -> PDDocument.load(path.toFile()),
                NoPrintServicePrinterJob::new
        );

        java.io.IOException error = assertThrows(java.io.IOException.class, () -> service.print(pdf));
        assertTrue(error.getMessage().contains("No system default printer is configured"));
    }

    private static class RecordingPrinterJob extends PrinterJob {
        private Pageable pageable;
        private boolean printCalled;

        @Override
        public void setPrintable(Printable painter) {
        }

        @Override
        public void setPrintable(Printable painter, PageFormat format) {
        }

        @Override
        public void setPageable(Pageable document) {
            this.pageable = document;
        }

        @Override
        public boolean printDialog() {
            return false;
        }

        @Override
        public PageFormat pageDialog(PageFormat page) {
            return page;
        }

        @Override
        public PageFormat defaultPage(PageFormat page) {
            return page;
        }

        @Override
        public PageFormat validatePage(PageFormat page) {
            return page;
        }

        @Override
        public void print() throws PrinterException {
            this.printCalled = true;
        }

        @Override
        public void setCopies(int copies) {
        }

        @Override
        public int getCopies() {
            return 1;
        }

        @Override
        public String getUserName() {
            return "test-user";
        }

        @Override
        public void setJobName(String jobName) {
        }

        @Override
        public String getJobName() {
            return "rail-print-test";
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public PageFormat defaultPage() {
            PageFormat format = new PageFormat();
            format.setPaper(new Paper());
            return format;
        }

        @Override
        public PrintService getPrintService() {
            return buildPrintServiceStub();
        }

        @Override
        public void setPrintService(PrintService service) {
        }
    }

    private static final class ThrowingPrinterJob extends RecordingPrinterJob {
        @Override
        public void print() throws PrinterException {
            throw new PrinterException("printer offline");
        }
    }

    private static final class NoPrintServicePrinterJob extends RecordingPrinterJob {
        @Override
        public PrintService getPrintService() {
            return null;
        }
    }

    private static PrintService buildPrintServiceStub() {
        return (PrintService) Proxy.newProxyInstance(
                RailPrintServiceTest.class.getClassLoader(),
                new Class<?>[]{PrintService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "SystemDefaultTestPrinter";
                    case "isDocFlavorSupported" -> true;
                    case "getSupportedDocFlavors" -> new DocFlavor[]{DocFlavor.SERVICE_FORMATTED.PAGEABLE};
                    case "getUnsupportedAttributes" -> null;
                    case "getSupportedAttributeCategories" -> new Class<?>[0];
                    case "isAttributeCategorySupported" -> false;
                    case "getSupportedAttributeValues" -> null;
                    case "isAttributeValueSupported" -> false;
                    case "getDefaultAttributeValue" -> null;
                    case "getAttributes" -> printServiceAttributes();
                    case "getAttribute" -> null;
                    case "createPrintJob" -> null;
                    case "getServiceUIFactory" -> null;
                    case "hashCode" -> 7;
                    case "equals" -> proxy == args[0];
                    case "toString" -> "SystemDefaultTestPrinter";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static PrintServiceAttributeSet printServiceAttributes() {
        HashPrintServiceAttributeSet attributes = new HashPrintServiceAttributeSet();
        attributes.add(new PrinterName("SystemDefaultTestPrinter", null));
        return attributes;
    }
}
