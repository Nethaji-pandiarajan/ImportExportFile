package com.example.fileuploaddownload.controller;

import com.example.fileuploaddownload.pojo.CodeRequest;
import com.example.fileuploaddownload.pojo.ExecutionResult;
import com.example.fileuploaddownload.service.RunTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/runtime")
public class RunTimeController {

    @Autowired
    private RunTimeService runTimeService;

    @PostMapping("/run")
    public ResponseEntity<ExecutionResult> run(@RequestBody CodeRequest req) {
        return ResponseEntity.ok(runTimeService.execute(req));
    }
}
