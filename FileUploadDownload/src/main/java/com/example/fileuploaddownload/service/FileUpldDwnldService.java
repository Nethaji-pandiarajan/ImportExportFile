package com.example.fileuploaddownload.service;

import com.example.fileuploaddownload.dto.ImportExportResponse;
import com.example.fileuploaddownload.pojo.ExportRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileUpldDwnldService {

    public ImportExportResponse importExcel(MultipartFile file, String tableName) throws Exception;

    public ImportExportResponse importCSV(MultipartFile file, String tableName) throws Exception;

    public void streamExport(ExportRequest request, String tableName, HttpServletResponse response) throws Exception;
}
