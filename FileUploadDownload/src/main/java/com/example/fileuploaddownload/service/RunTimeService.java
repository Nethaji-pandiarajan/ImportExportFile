package com.example.fileuploaddownload.service;

import com.example.fileuploaddownload.pojo.CodeRequest;
import com.example.fileuploaddownload.pojo.ExecutionResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface RunTimeService {

    public ExecutionResult execute(CodeRequest req);
}
