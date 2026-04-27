package com.project.bars.service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportProcessingService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    private final CloudinaryStorageService cloudinaryStorageService;
    private final PythonAnalysisService pythonAnalysisService;

    public ReportProcessingService(CloudinaryStorageService cloudinaryStorageService,
                                   PythonAnalysisService pythonAnalysisService) {
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.pythonAnalysisService = pythonAnalysisService;
    }

    public Map<String, Object> uploadAndAnalyze(MultipartFile file) {
        validatePdf(file);

        try {
            Map<String, Object> uploadResult = cloudinaryStorageService.uploadPdf(file);
            Map<String, Object> analysisResult = pythonAnalysisService.processPdf(file.getOriginalFilename(), file.getBytes());
            Map<String, Object> cloud = new LinkedHashMap<>();
            cloud.put("public_id", uploadResult.get("public_id"));
            cloud.put("asset_id", uploadResult.get("asset_id"));
            cloud.put("secure_url", uploadResult.get("secure_url"));
            cloud.put("format", uploadResult.get("format"));
            cloud.put("resource_type", uploadResult.get("resource_type"));

            Map<String, Object> response = new LinkedHashMap<>(analysisResult);
            response.put("cloud", cloud);
            return response;
        } catch (Exception exception) {
            throw new IllegalStateException("Backend report processing failed.", exception);
        }
    }

    public Map<String, Object> generateSummary(Map<String, Object> payload) {
        return pythonAnalysisService.generateSummary(payload);
    }

    public ResponseEntity<byte[]> downloadJson(String prefix, Map<String, Object> payload) {
        String filename = prefix + "_" + FILE_TS.format(Instant.now()) + ".json";
        byte[] content = toJson(payload).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(content);
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF uploads are supported.");
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJson(entry.getValue()));
            }

            return builder.append('}').toString();
        }

        if (value instanceof Collection<?> collectionValue) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;

            for (Object item : collectionValue) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(item));
            }

            return builder.append(']').toString();
        }

        return toJson(String.valueOf(value));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
