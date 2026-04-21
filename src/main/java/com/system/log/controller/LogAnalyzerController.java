package com.system.log.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.log.dto.LogAnalysisResponse;
import com.system.log.service.LogAnalyzerService;
import com.system.log.service.LogFileReaderService;

@RestController
@RequestMapping("/ai")
public class LogAnalyzerController {

    private final LogAnalyzerService analyzerService;
    private final LogFileReaderService readerService;

    public LogAnalyzerController(LogAnalyzerService analyzerService,
                                 LogFileReaderService readerService) {
        this.analyzerService = analyzerService;
        this.readerService = readerService;
    }

    @GetMapping("/analyze")
    public String analyzeLogs() throws Exception {

        String logs = readerService.readErrorLogs();

        return analyzerService.analyzeLogs(logs);
    }
    
    @GetMapping("/analyzeAll")
    public List<LogAnalysisResponse> analyzeAll() throws Exception {

        List<String> errorBlocks = readerService.extractErrorBlocks();

        return analyzerService.analyzeMultiple(errorBlocks);
    }
    
    @GetMapping("/analyzeGrouped")
    public List<LogAnalysisResponse> analyzeGrouped() throws Exception {

        List<String> errorBlocks = readerService.extractErrorBlocks();

        return analyzerService.analyzeGrouped(errorBlocks);
    }
}
