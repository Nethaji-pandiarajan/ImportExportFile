package com.example.fileuploaddownload.dto;


import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportExportResponse {
    private boolean success;
    private String message;
    private List<RowError> errors;

    // constructors & static builders
    public static ImportExportResponse success(String msg) {
        ImportExportResponse r = new ImportExportResponse(); r.success = true; r.message = msg; return r;
    }
    public static ImportExportResponse failure(String msg) {
        ImportExportResponse r = new ImportExportResponse(); r.success = false; r.message = msg; return r;
    }
    public static ImportExportResponse partialSuccess(String msg, List<RowError> errors) {
        ImportExportResponse r = new ImportExportResponse(); r.success = false; r.message = msg; r.errors = errors; return r;
    }
}



