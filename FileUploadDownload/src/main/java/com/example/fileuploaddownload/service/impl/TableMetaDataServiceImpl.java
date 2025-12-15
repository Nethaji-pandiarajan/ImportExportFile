package com.example.fileuploaddownload.service.impl;

import com.example.fileuploaddownload.pojo.ColumnMeta;
import com.example.fileuploaddownload.pojo.TableMeta;
import com.example.fileuploaddownload.service.TableMetaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Service
public class TableMetaDataServiceImpl implements TableMetaDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public TableMeta getTableMetadata(String tableName) {
        try {
            // Fetch columns
            String sqlCols = """
                    SELECT column_name, data_type, is_nullable, character_maximum_length
                    FROM information_schema.columns
                    WHERE table_name = ?
                    ORDER BY ordinal_position
                    """;
            List<Map<String, Object>> colRows = jdbcTemplate.queryForList(sqlCols, tableName);
            TableMeta meta = new TableMeta();
            meta.setTableName(tableName);

            for (Map<String, Object> r : colRows) {
                String col = r.get("column_name").toString();

                ColumnMeta c = new ColumnMeta();
                c.setName(col);
                c.setNullable(r.get("is_nullable").equals("YES"));
                c.setMaxLength(r.get("character_maximum_length") == null ? -1 :
                        Integer.parseInt(r.get("character_maximum_length").toString()));
                c.setSqlType(mapToSqlType(r.get("data_type").toString()));
                meta.getColumns().put(col, c);
                meta.getColumnOrder().add(col);
            }

            // UNIQUE COLUMNS
            String sqlUnique = """
                    SELECT kcu.column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                    WHERE tc.table_name = ? AND tc.constraint_type = 'UNIQUE'
                    """;
            meta.getUniqueColumns().addAll(jdbcTemplate.queryForList(sqlUnique, String.class, tableName));

            // FOREIGN KEYS
            String sqlFk = """
                    SELECT kcu.column_name, ccu.table_name AS ref_table, ccu.column_name AS ref_col
                    FROM information_schema.key_column_usage kcu
                    JOIN information_schema.constraint_column_usage ccu
                    ON kcu.constraint_name = ccu.constraint_name
                    JOIN information_schema.table_constraints tc
                    ON tc.constraint_name = kcu.constraint_name
                    WHERE tc.table_name = ? AND tc.constraint_type = 'FOREIGN KEY'
                    """;

            List<Map<String, Object>> fkRows = jdbcTemplate.queryForList(sqlFk, tableName);

            for (Map<String, Object> r : fkRows) {
                String col = r.get("column_name").toString();
                ColumnMeta c = meta.getColumns().get(col);
                c.setForeignRefTable(r.get("ref_table").toString());
                c.setForeignRefColumn(r.get("ref_col").toString());
            }
            return meta;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load table metadata", e);
        }

    }

    private int mapToSqlType(String type) {
        return switch (type.toLowerCase()) {
            case "int", "integer" -> Types.INTEGER;
            case "bigint" -> Types.BIGINT;
            case "date" -> Types.DATE;
            case "timestamp" -> Types.TIMESTAMP;
            case "boolean" -> Types.BOOLEAN;
            default -> Types.VARCHAR;
        };
    }
}
