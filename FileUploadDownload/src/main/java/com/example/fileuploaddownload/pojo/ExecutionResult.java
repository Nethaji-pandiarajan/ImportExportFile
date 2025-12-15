package com.example.fileuploaddownload.pojo;

public record ExecutionResult(
        String output,
        String error,
        long executionTimeMs
) {};
