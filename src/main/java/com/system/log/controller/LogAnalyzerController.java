package com.system.log.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.log.dto.LogAnalysisResponse;
import com.system.log.service.HtmlToPdfService;
import com.system.log.service.LogAnalyzerService;
import com.system.log.service.LogFileReaderService;

@RestController
@RequestMapping("/ai")
public class LogAnalyzerController {

	private final LogAnalyzerService analyzerService;
	private final LogFileReaderService readerService;
	private HtmlToPdfService htmlToPdfService;

	public LogAnalyzerController(LogAnalyzerService analyzerService, LogFileReaderService readerService,
			HtmlToPdfService htmlToPdfService) {
		this.analyzerService = analyzerService;
		this.readerService = readerService;
		this.htmlToPdfService = htmlToPdfService;
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

	@GetMapping("/report/pdf")
	public ResponseEntity<byte[]> generatePdf() throws Exception {

		List<String> logs = readerService.extractErrorBlocks();

		// 🔥 Proper HTML wrapper (VERY IMPORTANT)
		StringBuilder fullHtml = new StringBuilder("""
												<html>
												<head>
												<style>
								    body {
								        font-family: Arial, sans-serif;
								        padding: 20px;
								        background-color: #f5f7fa;
								    }

								    h1 {
								        color: #2c3e50;
								        border-bottom: 2px solid #ddd;
								        padding-bottom: 10px;
								    }

								    .issue {
								        background: #ffffff;
								        border-left: 6px solid #e74c3c;
								        padding: 15px;
								        margin-bottom: 25px;
								        border-radius: 8px;
								        box-shadow: 0px 2px 6px rgba(0,0,0,0.1);
								    }

								    .section-title {
								        font-weight: bold;
								        margin-top: 12px;
								        margin-bottom: 4px;
								        color: #2c3e50;
								    }

								    .label {
								        font-weight: bold;
								        color: #2c3e50;
								    }

								    .value {
								        margin-left: 5px;
								        display: inline-block;
								    }

								    .root-cause {
								        background: #fdecea;
								        padding: 10px;
								        border-radius: 5px;
								    }

								    .fix {
								        background: #eafaf1;
								        padding: 10px;
								        border-radius: 5px;
								    }

								    pre {
				     					background: #2d2d2d;
				    					color: #f8f8f2;
				    					padding: 12px;
				    					border-radius: 6px;
				    					font-size: 13px;
				    					overflow-x: auto;
				}
								</style>
								</head>

								<body>

								<h1>🚨 AI Log Analysis Report</h1>

								""");

		for (String log : logs) {

			// 🔥 Use HTML-based AI method
			String html = analyzerService.analyzeSingleForPdf(log);

			fullHtml.append(html).append("<hr/>");
		}

		fullHtml.append("""
				</body>
				</html>
				""");

		// 🔥 Convert HTML → PDF
		byte[] pdf = htmlToPdfService.convertHtmlToPdf(fullHtml.toString());

		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=ai-report.pdf")
				.contentType(MediaType.APPLICATION_PDF).contentLength(pdf.length).body(pdf);
	}
}
