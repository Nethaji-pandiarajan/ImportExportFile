package com.example.fileuploaddownload.pojo;

import lombok.*;

import java.util.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableMeta {
    private String tableName;
    private List<String> columnOrder = new ArrayList<>();
    private Map<String, ColumnMeta> columns = new LinkedHashMap<>();
    private List<String> uniqueColumns = new ArrayList<>();
}

