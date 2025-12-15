package com.example.fileuploaddownload.util;

import com.example.fileuploaddownload.pojo.ExportRequest;
import com.opencsv.CSVReader;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileUpldDwnldUtil {

    @Autowired
    static JdbcTemplate jdbcTemplate;

    public static class ImportResult {
        private final List<String> headers;
        private final List<Map<String, Object>> rows;

        public ImportResult(List<String> headers, List<Map<String, Object>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }

    public static void streamCsv(ExportRequest exportRequest, String tableName, HttpServletResponse response) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"export.csv\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // write header
            writer.write(String.join(",", exportRequest.selectColumns()));
            writer.newLine();

            // stream rows in batches
            streamRowsInBatches(exportRequest, row -> {
                try {
                    List<String> cells = exportRequest.selectColumns().stream().map(c -> Optional.ofNullable(row.get(c)).map(Object::toString).orElse("")).toList();
                    writer.write(String.join(",", escapeCsvCells(cells)));
                    writer.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, tableName);

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void streamExcel(ExportRequest exportRequest, String tableName, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"export.xlsx\"");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100); // keep 100 rows in memory
             ServletOutputStream out = response.getOutputStream()) {

            Sheet sheet = wb.createSheet("data");
            int[] rowNum = {0};
            // header
            Row header = sheet.createRow(rowNum[0]++);
            int ci = 0;
            for (String col : exportRequest.selectColumns()) header.createCell(ci++).setCellValue(col);

            // stream DB rows
            streamRowsInBatches(exportRequest, row -> {
                Row r = sheet.createRow(rowNum[0]++);
                int c = 0;
                for (String col : exportRequest.selectColumns()) {
                    Object v = row.get(col);
                    if (v instanceof Number) r.createCell(c++).setCellValue(((Number) v).doubleValue());
                    else r.createCell(c++).setCellValue(v != null ? v.toString() : "");
                }
            }, tableName);

            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void streamZip(ExportRequest exportRequest, String tableName, HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"export.zip\"");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // example: single csv entry; you can loop multiple datasets
            ZipEntry entry = new ZipEntry("export.csv");
            zos.putNextEntry(entry);

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8))) {
                writer.write(String.join(",", exportRequest.selectColumns()));
                writer.newLine();
                streamRowsInBatches(exportRequest, row -> {
                    try {
                        List<String> cells = exportRequest.selectColumns().stream().map(c -> Optional.ofNullable(row.get(c)).map(Object::toString).orElse("")).toList();
                        writer.write(String.join(",", escapeCsvCells(cells)));
                        writer.newLine();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, tableName);
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            zos.closeEntry();
            zos.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // ===================== CSV IMPORT =====================
    public static ImportResult importCSV(InputStream inputStream) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headerArr = reader.readNext();
            if (headerArr == null) throw new RuntimeException("Missing header row");
            List<String> headers = new ArrayList<>();
            for (String h : headerArr) headers.add(h.trim());

            List<Map<String, Object>> rows = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String v = i < line.length ? line[i] : "";
                    map.put(headers.get(i), v);
                }
                rows.add(map);
            }
            return new ImportResult(headers, rows);
        }
    }


    // ===================== EXCEL IMPORT =====================
    public static ImportResult importExcel(InputStream is) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new RuntimeException("Missing header row");
            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) headers.add(c.getStringCellValue().trim());

            List<Map<String, Object>> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Object> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    map.put(headers.get(c), getCellString(cell));
                }
                rows.add(map);
            }
            return new ImportResult(headers, rows);
        }
    }

    // ===================== Helper for Excel Cell Type =====================
    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toString();
                return Double.toString(cell.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // a small helper to escape CSV cells (very basic)
    private static List<String> escapeCsvCells(List<String> cells) {
        return cells.stream().map(cell -> {
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                cell = cell.replace("\"", "\"\"");
                return "\"" + cell + "\"";
            }
            return cell;
        }).toList();
    }

    // --- DB streaming / batching pattern ---
    private static void streamRowsInBatches(ExportRequest exportRequest, Consumer<Map<String, Object>> consumeRow, String tableName) {
        // Example: keyset paging approach, assuming table has an incremental id column called id
        // Adapt SQL-builder here for dynamic columns / filters.
        final int batchSize = 2000;
        long lastId = 0L;
        while (true) {
            DynamicQueryBuilderUtil.SqlAndParams queryParams = DynamicQueryBuilderUtil.build(tableName, exportRequest);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(queryParams.sql(), queryParams.params());
            if (rows.isEmpty()) break;
            for (Map<String, Object> r : rows) {
                consumeRow.accept(r);
            }
            // update lastId based on the last row (assumes 'id' present)
            Object maybeId = rows.get(rows.size() - 1).get("id");
            if (maybeId instanceof Number) lastId = ((Number) maybeId).longValue();
            else break;
            if (rows.size() < batchSize) break;
        }
    }


}

