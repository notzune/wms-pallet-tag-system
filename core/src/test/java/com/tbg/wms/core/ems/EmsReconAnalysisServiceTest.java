package com.tbg.wms.core.ems;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmsReconAnalysisServiceTest {

    @Test
    void parsesLegacyWorkbookAndClassifiesCommonMismatchPatterns() throws Exception {
        Path workbookPath = Files.createTempFile("ems-recon-", ".xls");
        try {
            writeWorkbook(workbookPath);

            List<EmsReconRow> rows = new EmsReconReportParser().parse(workbookPath);
            EmsReconAnalysisReport report = new EmsReconAnalysisService().analyze(workbookPath.toString(), rows, null);

            assertEquals(3, rows.size());
            assertEquals(3, report.getTotalFindings());
            assertEquals(EmsMismatchCategory.EMS_ONLY, report.getFindings().get(0).getCategory());
            assertEquals(EmsMismatchCategory.LOCATION_DRIFT, report.getFindings().get(1).getCategory());
            assertEquals(EmsMismatchCategory.WMS_ONLY, report.getFindings().get(2).getCategory());
            assertTrue(report.getNotes().get(0).contains("Parsed 3 mismatch rows"));
        } finally {
            Files.deleteIfExists(workbookPath);
        }
    }

    private void writeWorkbook(Path workbookPath) throws Exception {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("WM2012 - EMS RECONCILIATION");
            sheet.createRow(1).createCell(0).setCellValue("1st Pass -- Cranes 1,2, & 3");
            Row header = sheet.createRow(5);
            header.createCell(0).setCellValue("EMS");
            header.createCell(1).setCellValue("currentLocationId");
            header.createCell(2).setCellValue("containerId");
            header.createCell(3).setCellValue("sku");
            header.createCell(8).setCellValue("ASRS_LOC_NUM");
            header.createCell(10).setCellValue("LODNUM");
            header.createCell(11).setCellValue("MAX(PRTNUM)");

            Row emsOnly = sheet.createRow(7);
            emsOnly.createCell(0).setCellValue("EMS");
            emsOnly.createCell(1).setCellValue("UL010101211");
            emsOnly.createCell(2).setCellValue("76332317");
            emsOnly.createCell(3).setCellValue("10048500205874000");

            Row locationDrift = sheet.createRow(8);
            locationDrift.createCell(0).setCellValue("EMS");
            locationDrift.createCell(1).setCellValue("UL020202211");
            locationDrift.createCell(2).setCellValue("88888888");
            locationDrift.createCell(3).setCellValue("10048500205874000");
            locationDrift.createCell(8).setCellValue("UL999999999");
            locationDrift.createCell(10).setCellValue("LOD1");
            locationDrift.createCell(11).setCellValue("PRT1");

            Row wmsOnly = sheet.createRow(9);
            wmsOnly.createCell(8).setCellValue("UL030303333");
            wmsOnly.createCell(10).setCellValue("LOD2");
            wmsOnly.createCell(11).setCellValue("PRT2");

            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
    }
}
