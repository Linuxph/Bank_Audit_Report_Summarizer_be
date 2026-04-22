package com.project.bars.controllers;

import com.project.bars.dto.ReportSummaryRequest;
import com.project.bars.dto.ReportSummaryResponse;
import com.project.bars.service.ReportSummaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportSummaryService reportSummaryService;

    public ReportController(ReportSummaryService reportSummaryService) {
        this.reportSummaryService = reportSummaryService;
    }

    @GetMapping("/me")
    public Map<String, String> currentUser(Authentication authentication) {
        return Map.of(
                "message", "Authenticated request successful",
                "username", authentication.getName()
        );
    }

    @PostMapping("/summarize")
    public ReportSummaryResponse summarizeReport(@Valid @RequestBody ReportSummaryRequest request) {
        return reportSummaryService.summarize(request);
    }
}
