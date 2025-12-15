package com.example.fileuploaddownload.controller;

import com.example.fileuploaddownload.dto.ImportExportResponse;
import com.example.fileuploaddownload.pojo.ExportRequest;
import com.example.fileuploaddownload.service.FileUpldDwnldService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

    @RestController
    @RequiredArgsConstructor
    public class FileUpldDwnldController {

        @Autowired
        private final FileUpldDwnldService excelCsvService;

        @PostMapping("/importExcel")
        public ResponseEntity<ImportExportResponse> importExcel(@RequestParam("file") MultipartFile file, @PathVariable("tableName") String tableName) throws Exception {
            try {
                ImportExportResponse resp = excelCsvService.importExcel(file,tableName);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ImportExportResponse.failure(e.getMessage()));
            }
        }

        @PostMapping("/importCSV")
        public ResponseEntity<ImportExportResponse> importCSV(@RequestParam("file") MultipartFile file,@PathVariable("tableName") String tableName) throws Exception {
            try {
                ImportExportResponse resp = excelCsvService.importCSV(file,tableName);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ImportExportResponse.failure(e.getMessage()));
            }
        }

        @GetMapping("/streamExport")
        public void  streamExport(@RequestBody ExportRequest request, @PathVariable("tableName") String tableName, HttpServletResponse response) throws Exception {
            excelCsvService.streamExport(request,tableName, response);
        }

    }
