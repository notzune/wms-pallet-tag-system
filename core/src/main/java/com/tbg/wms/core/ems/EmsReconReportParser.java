package com.tbg.wms.core.ems;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Parses the legacy XLS workbook produced by the EMS reconciliation job.
 */
public final class EmsReconReportParser {
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);

    public List<EmsReconRow> parse(Path workbookPath) throws IOException {
        Objects.requireNonNull(workbookPath, "workbookPath cannot be null");
        if (!Files.exists(workbookPath)) {
            throw new IOException("Workbook does not exist: " + workbookPath);
        }

        try (InputStream inputStream = Files.newInputStream(workbookPath);
             Workbook workbook = new HSSFWorkbook(inputStream)) {
            List<EmsReconRow> rows = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                parseSheet(workbook.getSheetAt(sheetIndex), rows);
            }
            return rows;
        }
    }

    private void parseSheet(Sheet sheet, List<EmsReconRow> rows) {
        String currentPassLabel = "";
        int passIndex = 0;
        boolean insideDetailBlock = false;

        for (Row row : sheet) {
            String c0 = cell(row, 0);
            String c1 = cell(row, 1);
            String c2 = cell(row, 2);

            if (c0.startsWith("WM2012 - EMS RECONCILIATION")) {
                insideDetailBlock = false;
                continue;
            }

            if (looksLikePassLabel(c0)) {
                passIndex++;
                currentPassLabel = c0;
                insideDetailBlock = false;
                continue;
            }

            if ("EMS".equalsIgnoreCase(c0)
                    && "currentLocationId".equalsIgnoreCase(c1)
                    && "containerId".equalsIgnoreCase(c2)) {
                insideDetailBlock = true;
                continue;
            }

            if (!insideDetailBlock) {
                continue;
            }

            if (c0.startsWith("WM2012 - EMS RECONCILIATION")) {
                insideDetailBlock = false;
                continue;
            }

            EmsReconRow parsed = toReconRow(row, currentPassLabel, passIndex);
            if (parsed != null) {
                rows.add(parsed);
            }
        }
    }

    private EmsReconRow toReconRow(Row row, String passLabel, int passIndex) {
        String currentLocationId = cell(row, 1);
        String containerId = cell(row, 2);
        String sku = cell(row, 3);
        String asrsLocNum = cell(row, 8);
        String lodnum = cell(row, 10);
        String maxPrtnum = cell(row, 11);

        boolean hasRelevantData = !(currentLocationId.isEmpty() && containerId.isEmpty() && sku.isEmpty()
                && asrsLocNum.isEmpty() && lodnum.isEmpty() && maxPrtnum.isEmpty());
        if (!hasRelevantData) {
            return null;
        }

        String resolvedPassLabel = passLabel.isEmpty() ? "Pass " + Math.max(1, passIndex) : passLabel;
        return new EmsReconRow(
                resolvedPassLabel,
                Math.max(1, passIndex),
                row.getRowNum() + 1,
                currentLocationId,
                containerId,
                sku,
                asrsLocNum,
                lodnum,
                maxPrtnum
        );
    }

    private boolean looksLikePassLabel(String value) {
        String lowered = value.toLowerCase(Locale.ROOT);
        return lowered.contains("pass") && lowered.contains("crane");
    }

    private String cell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }
}
