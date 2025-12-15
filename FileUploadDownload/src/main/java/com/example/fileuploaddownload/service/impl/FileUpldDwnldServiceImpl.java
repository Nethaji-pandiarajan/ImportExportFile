package com.example.fileuploaddownload.service.impl;

import com.example.fileuploaddownload.dao.BatchInsertDao;
import com.example.fileuploaddownload.dto.ImportExportResponse;
import com.example.fileuploaddownload.dto.RowError;
import com.example.fileuploaddownload.pojo.ColumnMeta;
import com.example.fileuploaddownload.pojo.ExportRequest;
import com.example.fileuploaddownload.pojo.TableMeta;
import com.example.fileuploaddownload.service.FileUpldDwnldService;
import com.example.fileuploaddownload.service.TableMetaDataService;
import com.example.fileuploaddownload.util.FileUpldDwnldUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FileUpldDwnldServiceImpl implements FileUpldDwnldService {

    @Autowired
    TableMetaDataService metadataService;

    @Autowired
    BatchInsertDao batchInsertDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ImportExportResponse importExcel(MultipartFile file, String tableName) throws Exception {
        FileUpldDwnldUtil.ImportResult res = FileUpldDwnldUtil.importExcel(file.getInputStream());
        return processImport(res.getHeaders(), res.getRows(), tableName);
    }

    @Override
    public ImportExportResponse importCSV(MultipartFile file, String tableName) throws Exception {
        FileUpldDwnldUtil.ImportResult res = FileUpldDwnldUtil.importCSV(file.getInputStream());
        return processImport(res.getHeaders(), res.getRows(), tableName);
    }

    @Override
    public void streamExport(ExportRequest exportRequest, String tableName, HttpServletResponse response) throws Exception {
        String fmt = exportRequest.format().toLowerCase();
        switch (fmt) {
            case "csv" -> FileUpldDwnldUtil.streamCsv(exportRequest, tableName, response);
            case "excel" -> FileUpldDwnldUtil.streamExcel(exportRequest, tableName, response);
            case "zip" -> FileUpldDwnldUtil.streamZip(exportRequest, tableName, response);
            default -> throw new IllegalArgumentException("unsupported format: " + fmt);
        }
    }

    private ImportExportResponse processImport(List<String> headers, List<Map<String, Object>> rows, String tableName) {
        TableMeta meta = metadataService.getTableMetadata(tableName);

        List<String> fileHeaders = headers.stream()
                .map(String::trim)
                .toList();

        if (!fileHeaders.equals(meta.getColumnOrder())) {
            return ImportExportResponse.failure("Header mismatch. Expected: " + meta.getColumnOrder());
        }

        // iterate rows, chunk them, validate and send to worker
        List<RowError> errors = new ArrayList<>();
        List<Map<String, Object>> dataList = new ArrayList<>();
        int rowNum = 1; // header is row 0; user rows start at 1 (or 2 if you want absolute)

        for (Map<String, Object> row : rows) {
            rowNum++;
            try {
                validateRow(row, meta, rowNum, errors); // may add to errors
                dataList.add(row);
            } catch (RuntimeException e) {
                errors.add(new RowError(rowNum, e.getMessage(), row));
            }
        }

        if (!errors.isEmpty()) {
            return ImportExportResponse.partialSuccess(errors.size() + " row errors", errors);
        }
        batchInsertDao.batchInsert(tableName, dataList);
        return ImportExportResponse.success("Imported successfully");
    }


    private void validateRow(Map<String, Object> row, TableMeta meta, int rowNum, List<RowError> errors) {
        for (Map.Entry<String, ColumnMeta> entry : meta.getColumns().entrySet()) {
            String col = entry.getKey();
            ColumnMeta cm = entry.getValue();
            String val = String.valueOf(row.getOrDefault(col, "")).trim();
            Object typedValue = null;
            // NOT NULL
            if (!cm.isNullable() && val.isEmpty()) {
                throw new RuntimeException("Required column '" + col + "' is blank at row " + rowNum);
            }

            // LENGTH
            if (cm.getMaxLength() > 0 && val.length() > cm.getMaxLength()) {
                throw new RuntimeException("Column '" + col + "' exceeds max length " + cm.getMaxLength() + " at row " + rowNum);
            }

            // TYPE VALIDATION
            try {
                typedValue = validateType(val, cm);
            } catch (Exception ex) {
                throw new RuntimeException("Column '" + col + "' type mismatch at row " + rowNum + ": " + ex.getMessage());
            }

            // FOREIGN KEY VALIDATION
            if (cm.getForeignRefTable() != null) {
                if (!existsInForeignTable(cm, typedValue))
                    throw new RuntimeException("Invalid foreign key for column '" + col + "'");
            }

            // UNIQUE VALIDATION
            for (String ucol : meta.getUniqueColumns()) {
                Object value = row.get(ucol);
                if (existsDuplicate(meta.getTableName(), ucol, value))
                    throw new RuntimeException("Duplicate fieldValue violates UNIQUE constraint for column '" + ucol + "'");
            }

        }
    }

    // ============================
    // FOREIGN KEY CHECK
    // ============================
    private boolean existsInForeignTable(ColumnMeta colMeta, Object value) {
        String sql = "SELECT COUNT(*) FROM " + colMeta.getForeignRefTable() +
                " WHERE " + colMeta.getForeignRefColumn() + " = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
    }

    // ============================
    // UNIQUE CHECK
    // ============================
    private boolean existsDuplicate(String table, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
    }

    private Object validateType(String value, ColumnMeta meta) throws Exception {
        if (value.isEmpty()) return null;

        return switch (meta.getSqlType()) {
            case Types.INTEGER -> Integer.parseInt(value);
            case Types.BIGINT -> Long.parseLong(value);
            case Types.BOOLEAN -> Boolean.parseBoolean(value);
            case Types.DATE -> java.sql.Date.valueOf(value);
            case Types.TIMESTAMP -> java.sql.Timestamp.valueOf(value);
            default -> value;
        };
    }

}
