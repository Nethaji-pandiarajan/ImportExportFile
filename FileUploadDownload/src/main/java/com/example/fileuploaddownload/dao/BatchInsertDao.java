package com.example.fileuploaddownload.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class BatchInsertDao {

    private final JdbcTemplate jdbc;

    public BatchInsertDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int batchInsert(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        // use insertion columns from first row (preserve order)
        LinkedHashSet<String> colSet = new LinkedHashSet<>(rows.get(0).keySet());
        List<String> cols = new ArrayList<>(colSet);

        String colCsv = String.join(",", cols);
        String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(","));
        String sql = "INSERT INTO " + tableName + " (" + colCsv + ") VALUES (" + placeholders + ")";
        return batchInsertEfficient(sql, cols, rows, 2000);
    }


    public int batchInsertEfficient(String sql, List<String> columns, List<Map<String, Object>> rows, int batchSize) {

        int totalInserted = 0;

        for (int i = 0; i < rows.size(); i += batchSize) {

            int end = Math.min(i + batchSize, rows.size());
            List<Map<String, Object>> batch = rows.subList(i, end);

            int[][] status = jdbc.batchUpdate(sql, batch, batchSize, (ps, row) -> {
                int idx = 1;
                for (String col : columns) {
                    ps.setObject(idx++, row.get(col));
                }
            });

            totalInserted = Arrays.stream(status).flatMapToInt(Arrays::stream).sum();
        }

        return totalInserted;
    }


}
