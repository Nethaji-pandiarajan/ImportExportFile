package com.example.fileuploaddownload.util;

import com.example.fileuploaddownload.pojo.ExportRequest;
import com.example.fileuploaddownload.pojo.Filter;

import java.util.*;

public class DynamicQueryBuilderUtil {

    public record SqlAndParams(String sql, List<Object> params) {
    }

    public static SqlAndParams build(String tableName, ExportRequest req) {

        StringBuilder sql = new StringBuilder("SELECT ");

        // 🔥 dynamic select columns
        if (req.selectColumns() == null || req.selectColumns().isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(",", req.selectColumns()));
        }

        sql.append(" FROM ").append(tableName).append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        // 🔥 dynamic WHERE filters
        if (req.filters() != null) {
            for (Filter f : req.filters()) {

                switch (f.operator()) {

                    case EQ -> {
                        sql.append(" AND ").append(f.fieldName()).append(" = ? ");
                        params.add(f.fieldValue());
                    }

                    case LIKE -> {
                        sql.append(" AND ").append(f.fieldName()).append(" LIKE ? ");
                        params.add("%" + f.fieldValue() + "%");
                    }

                    case GT -> {
                        sql.append(" AND ").append(f.fieldName()).append(" > ? ");
                        params.add(f.fieldValue());
                    }
                    case GTE -> {
                        sql.append(" AND ").append(f.fieldName()).append(" >= ? ");
                        params.add(f.fieldValue());
                    }
                    case LT -> {
                        sql.append(" AND ").append(f.fieldName()).append(" < ? ");
                        params.add(f.fieldValue());
                    }
                    case LTE -> {
                        sql.append(" AND ").append(f.fieldName()).append(" <= ? ");
                        params.add(f.fieldValue());
                    }

                    case BETWEEN -> {
                        List<?> list = (List<?>) f.fieldValue();
                        sql.append(" AND ").append(f.fieldName()).append(" BETWEEN ? AND ? ");
                        params.add(list.get(0));
                        params.add(list.get(1));
                    }

                    case IN -> {
                        List<?> list = (List<?>) f.fieldValue();
                        sql.append(" AND ").append(f.fieldName()).append(" IN (")
                                .append("?,".repeat(list.size()));
                        sql.setLength(sql.length() - 1);
                        sql.append(")");
                        params.addAll(list);
                    }

                    case NOT_IN -> {
                        List<?> list = (List<?>) f.fieldValue();
                        sql.append(" AND ").append(f.fieldName()).append(" NOT IN (")
                                .append("?,".repeat(list.size()));
                        sql.setLength(sql.length() - 1);
                        sql.append(")");
                        params.addAll(list);
                    }

                    case IS_NULL -> sql.append(" AND ").append(f.fieldName()).append(" IS NULL ");
                    case IS_NOT_NULL -> sql.append(" AND ").append(f.fieldName()).append(" IS NOT NULL ");
                }
            }
        }

        // 🔥 Sorting
        if (req.sort() != null && req.sort().field() != null) {
            sql.append(" ORDER BY ")
                    .append(req.sort().field()).append(" ")
                    .append(req.sort().direction() != null ? req.sort().direction() : "ASC");
        }

        // 🔥 Pagination
        if (req.page() != null && req.size() != null) {
            int offset = (req.page() - 1) * req.size();
            sql.append(" LIMIT ").append(req.size()).append(" OFFSET ").append(offset);
        }

        return new SqlAndParams(sql.toString(), params);
    }
}
