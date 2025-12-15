package com.example.fileuploaddownload.pojo;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMeta {
    private String name;
    private boolean nullable;
    private int maxLength;
    private int sqlType;

    private String foreignRefTable;
    private String foreignRefColumn;
}

