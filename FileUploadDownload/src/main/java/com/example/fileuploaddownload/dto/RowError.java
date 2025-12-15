package com.example.fileuploaddownload.dto;

import lombok.*;

import java.util.Map;

@Data
@Getter
@Setter
public class RowError {
    private int rowNumber;
    private String error;
    private Map<String,Object> rowData;
    public RowError() {}
    public RowError(int rowNumber, String error, Map<String,Object> rowData) {
        this.rowNumber = rowNumber; this.error = error; this.rowData = rowData;
    }
    // getters/setters
}
