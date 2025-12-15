package com.example.fileuploaddownload.service.impl;

import com.example.fileuploaddownload.pojo.CodeRequest;
import com.example.fileuploaddownload.pojo.ExecutionResult;
import com.example.fileuploaddownload.service.RunTimeService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class RunTimeServiceImpl implements RunTimeService {

    private static final long DEFAULT_TIMEOUT_MS = 5000; // 5s, tune per language
    private static final ExecutorService IO_POOL = Executors.newCachedThreadPool();

    public ExecutionResult execute(CodeRequest req) {
        String container = switch (req.language().toLowerCase()) {
            case "python" -> "python:3.11";
            case "java" -> "eclipse-temurin:17";
            case "c" -> "gcc:latest";
            case "cpp", "c++" -> "gcc:latest";
            case "node", "javascript", "js" -> "node:20";
            case "php" -> "php:8.2-cli";
            case "ruby" -> "ruby:3.2";
            case "r" -> "r-base:latest";
            case "go", "golang" -> "golang:1.22";
            default -> throw new IllegalArgumentException("Unsupported language: " + req.language());
        };

        return runInDocker(container, req);
    }



    private ExecutionResult runInDocker(String image, CodeRequest req)  {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("code");
            Path codeFile = tempDir.resolve(getFileName(req.language()));
            Files.writeString(codeFile, req.sourceCode());
            String buildCmd = buildCommand(req.language(), codeFile.getFileName().toString());
            String runCmd   = runCommand(req.language(), codeFile.getFileName().toString());
            String hostDir = tempDir.toAbsolutePath().toString();

            String shellCommand = buildCmd + " && " + runCmd;

            List<String> cmd = List.of(
                    "docker", "run",
                    "--rm",
                    "--network=none",
                    "-i",
                    "--memory=256m",
                    "--cpus=0.5",
                    "--pids-limit=64",
                    "--security-opt=no-new-privileges",
                    "-v", hostDir + ":/app:rw",
                    "--read-only",
                    "--tmpfs", "/tmp:rw,exec,size=64m",
                    image,
                    "sh", "-c", shellCommand
            );


            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false); // we will read separately
            Process process = pb.start();

            Future<?> stdinWriter = IO_POOL.submit(() -> {
                try (OutputStream os = process.getOutputStream()) {
                    if (req.input() != null && !req.input().isEmpty()) {
                        os.write(req.input().getBytes());
                        os.flush();
                    }
                } catch (IOException e) {
                    // ignore
                }
            });

            ByteArrayOutputStream stdoutBaos = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBaos = new ByteArrayOutputStream();

            Future<?> stdoutFuture = IO_POOL.submit(() -> {
                try (InputStream is = process.getInputStream()) {
                    is.transferTo(stdoutBaos);
                } catch (IOException ignored) {}
            });

            Future<?> stderrFuture = IO_POOL.submit(() -> {
                try (InputStream is = process.getErrorStream()) {
                    is.transferTo(stderrBaos);
                } catch (IOException ignored) {}
            });

            boolean finished;
            long toMs = 5000 > 0 ? 5000 : DEFAULT_TIMEOUT_MS;
            finished = process.waitFor(toMs, TimeUnit.MILLISECONDS);
            boolean timedOut = false;
            int exitCode = -1;
            if (!finished) {
                timedOut = true;
                process.destroyForcibly();
            } else {
                exitCode = process.exitValue();
            }

            // wait for readers to finish (give small grace)
            try {
                stdoutFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
            try {
                stderrFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
            try {
                stdinWriter.get(200, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}

            String stdout = stdoutBaos.toString();
            String stderr = stderrBaos.toString();
            long runtimeMs = toMs; // approximate — you can add accurate timing if needed

            return new ExecutionResult(stdout, stderr, runtimeMs);
        } catch (Exception ex) {
            return new ExecutionResult("", ex.getMessage(), 0);
        } finally {
            try { Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); } catch (Exception ignored) {}

        }
    }

    private String getFileName(String lang) {
        return switch (lang.toLowerCase()) {
            case "python" -> "main.py";
            case "java" -> "Main.java";
            case "c" -> "main.c";
            case "cpp", "c++" -> "main.cpp";
            case "node", "javascript", "js" -> "main.js";
            case "php" -> "main.php";
            case "ruby" -> "main.rb";
            case "r" -> "main.r";
            case "go", "golang" -> "main.go";
            default -> "code.txt";
        };
    }

    public String buildCommand(String language, String fileName) {
        return switch (language.toLowerCase()) {

            // ---------------- JAVA ----------------
            case "java" -> String.format("javac /app/%s", fileName);

            // ---------------- PYTHON ----------------
            // no compilation needed
            case "python", "py" -> "echo \"Python has no build step\"";

            // ---------------- C ----------------
            case "c" -> String.format("gcc /app/%s -o /app/app.out", fileName);

            // ---------------- C++ ----------------
            case "cpp", "c++" -> String.format("g++ /app/%s -o /app/app.out", fileName);

            // ---------------- GO ----------------
            case "go", "golang" -> String.format("go build -o /app/app.out /app/%s", fileName);

            // ---------------- PHP ----------------
            case "php" -> "echo \"PHP has no build step\"";

            // ---------------- RUBY ----------------
            case "ruby" -> "echo \"Ruby has no build step\"";

            // ---------------- R ----------------
            case "r" -> "echo \"R has no build step\"";

            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    public String runCommand(String language, String fileName) {
        return switch (language.toLowerCase()) {

            // ---------------- JAVA ----------------
            case "java" -> "java -cp /app Main";

            // ---------------- PYTHON ----------------
            case "python", "py" -> String.format("python3 /app/%s", fileName);

            // ---------------- C++ ----------------
            case "c", "cpp", "c++", "go", "golang" -> "/app/app.out";

            // ---------------- PHP ----------------
            case "php" -> String.format("php /app/%s", fileName);

            // ---------------- RUBY ----------------
            case "ruby" -> String.format("ruby /app/%s", fileName);

            // ---------------- R ----------------
            case "r" -> String.format("Rscript /app/%s", fileName);

            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }



}
