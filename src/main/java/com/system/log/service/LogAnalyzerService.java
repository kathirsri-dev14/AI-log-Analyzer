package com.system.log.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.log.dto.LogAnalysisResponse;
import com.system.log.extractor.MethodExtractor;
import com.system.log.parser.StackTraceInfo;
import com.system.log.parser.StackTraceParser;
import com.system.log.resolver.SourceCodeResolver;

@Service
public class LogAnalyzerService {

	@Autowired
	private StackTraceParser parser;
	@Autowired
	private SourceCodeResolver resolver;

	@Autowired
	private MethodExtractor methodExtractor;

	private ChatClient chatClient;

	public LogAnalyzerService(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	public String analyzeLogs(String logs) {

		// ✅ Step 1: Parse stack trace
		StackTraceInfo info = parser.parse(logs);
		String methodCode = null;

		if (info != null) {
			String filePath = resolver.resolve(info.className());
			methodCode = methodExtractor.extractMethod(filePath, info.lineNumber());
			// String faultyLine = resolver.getFaultyLine(filePath, info.lineNumber());
		} else {
			System.out.println("No stack trace found");
		}

		String prompt = buildPrompt(logs, methodCode);
		return chatClient.prompt().user(u -> u.text(prompt)).call().content();
	}

	public List<LogAnalysisResponse> analyzeMultiple(List<String> logsList) {

		return logsList.stream().map(this::analyzeSingle).toList();
	}

	private LogAnalysisResponse analyzeSingle(String logs) {

		StackTraceInfo info = parser.parse(logs);

		System.out.println("==== LOG BLOCK START ====");
		System.out.println(logs);
		System.out.println("==== LOG BLOCK END ====");

		String methodCode = null;
		String faultyLine = "Not available";
		String className = "Unknown";
		String methodName = "Unknown";
		int lineNumber = 0;

		if (info != null) {
			className = info.className();
			methodName = info.methodName();
			lineNumber = info.lineNumber();

			String filePath = resolver.resolve(info.className());
			methodCode = methodExtractor.extractMethod(filePath, info.lineNumber());
			faultyLine = resolver.getFaultyLine(filePath, info.lineNumber());
		} else {
			System.out.println("No stack trace → fallback mode");
		}

		String prompt = buildPrompt(logs, methodCode);

		String response = chatClient.prompt().user(u -> u.text(prompt)).call().content();

		System.out.println("RAW AI RESPONSE:\n" + response);

		// return parseManually(response);
		// 🔥 Parse AI response
		LogAnalysisResponse ai = parseManually(response);

		// 🔥 OVERRIDE with real values (IMPORTANT)
		return new LogAnalysisResponse(ai.errorType(), ai.occurrences(), ai.topIssue(), ai.rootCause(), ai.severity(),
				className, // ✅ real
				methodName, // ✅ real
				lineNumber, // ✅ real
				faultyLine, // 🔥 real (not AI)
				ai.suggestedFix(), ai.exampleSolution());
	}

	public String analyzeSingleForPdf(String logs) {

		StackTraceInfo info = parser.parse(logs);

		String methodCode = null;

		if (info != null) {
			String filePath = resolver.resolve(info.className());
			methodCode = methodExtractor.extractMethod(filePath, info.lineNumber());
		}

		// 🔥 HERE you use HTML prompt
		String htmlPrompt = buildHtmlPrompt(logs, methodCode);

		String html = chatClient.prompt().user(u -> u.text(htmlPrompt)).call().content();
		html = html.replaceAll("^[^<]+", "");
		//html = html.replaceAll("class=\"[^\"]*\"", "class=\"value\"");
		 html = cleanWithJsoup(html);
		return html;
	}

	private String buildPrompt(String logs, String methodCode) {

		return """
				You are a senior production support engineer and code fixer.

				Your task is to analyze logs and provide a precise, machine-actionable fix that can be directly used by an AI coding agent (like GitHub Copilot).

				STRICT OUTPUT RULES:
				- Output MUST be valid JSON
				- Do NOT include markdown, comments, or extra text outside JSON
				- Do NOT wrap JSON in code blocks
				- Use EXACT field names provided
				- Keep response concise and accurate

				REQUIRED JSON FORMAT:

				{
				  "errorType": "",
				  "rootCause": "",
				  "severity": "Low | Medium | High | Critical",
				  "className": "",
				  "methodName": "",
				  "lineNumber": 0,
				  "faultyLine": "",
				  "suggestedFix": "",
				  "exampleSolution": ""
				}

				FIELD RULES:

				1. errorType:
				- Extract exact exception name from log
				- Always return fully qualified name (e.g., java.lang.NullPointerException)

				2. className:
				- MUST be extracted ONLY from stack trace
				- DO NOT guess or generate new class names

				3. methodName:
				- MUST be extracted ONLY from stack trace
				- DO NOT guess

				4. lineNumber:
				- MUST be extracted from stack trace

				5. faultyLine:
				- Identify EXACT line from provided source code
				- Must match lineNumber
				- Do NOT generate or assume code if not present

				6. rootCause:
				- Explain technical reason for failure
				- Include impact (e.g., crash, API failure, incorrect data)

				7. suggestedFix:
				- Provide short and precise fix (1–2 lines)

				8. exampleSolution (CRITICAL FOR AGENT MODE):
				- Provide corrected code snippet
				- MUST include:
				    - Original faulty line
				    - Fixed version
				- Clearly mark:
				    // ❌ FAULTY LINE
				    // ✅ FIXED LINE
				- Keep code clean and properly formatted
				- Do NOT add unnecessary indentation
				- Ensure fix can be directly applied
				- Use \\n for line breaks

				IMPORTANT RULES:
				- Use BOTH log and source code for analysis
				- DO NOT return "Unknown" if information is available
				- DO NOT guess missing values
				- DO NOT hallucinate class or method names
				- Prefer accuracy over verbosity

				INPUT LOG:
				"""
				+ logs + """

						SOURCE CODE:
						""" + (methodCode != null ? methodCode : "Not Available");
	}

	private String buildHtmlPrompt(String logs, String methodCode) {

		return """
				You are a senior production support engineer.

				Generate a professional issue report in CLEAN HTML format.

				STRICT RULES:
				- Output MUST be valid HTML
				- Output MUST start with <div class="issue">
				- Output MUST end with </div>
				- Do NOT use markdown (#, **, etc.)
				- NEVER use '#' for headings
				- ALWAYS use <h2> for titles
				- Keep HTML structure valid and well-formed
				- Do NOT include invalid characters inside attributes
				- Avoid using generics like <String> in output
				- NEVER use '#' for headings
				- ALWAYS use <h2>Issue Report</h2>
				- If '#' appears, convert it to <h2>
				

				REQUIRED FORMAT:

				<div class="issue">

				<h2>🚨 Issue Report</h2>

				<p><span class="label">Error Type:</span> <span class="value">...</span></p>
				<p><span class="label">Class Name:</span> <span class="value">...</span></p>
				<p><span class="label">Method Name:</span> <span class="value">...</span></p>
				<p><span class="label">Line Number:</span> <span class="value">...</span></p>

				<div class="root-cause">
				<p><b>Root Cause:</b> ...</p>
				<p><b>Impact:</b> ...</p>
				</div>

				<div class="fix">
				<p><b>Suggested Fix:</b> ...</p>
				</div>

				<p><b>Example Solution:</b></p>

				<pre>
				code here
				</pre>

				</div>

				IMPORTANT:
				- Use BOTH log and source code
				- Do NOT hallucinate class or method names
				- If data is missing, use "Not Available"
				- Keep content concise and readable

				LOG:
				""" + logs + """

				SOURCE CODE:
				""" + (methodCode != null ? methodCode : "Not Available");
	}

	// extract key to find the error
	private String extractKey(String log) {

		// 1️ Extract Exception class (best case)
		Pattern pattern = Pattern.compile("([a-zA-Z0-9_.]+Exception)");
		Matcher matcher = pattern.matcher(log);

		if (matcher.find()) {
			return matcher.group(1);
		}

		// 2️ Extract meaningful message from ERROR line
		String[] lines = log.split("\n");

		for (String line : lines) {
			if (line.contains("ERROR")) {

				// Remove log prefix (timestamp, class, etc.)
				String cleaned = line.replaceAll(".*ERROR.*?:", "").trim();

				// Normalize spaces
				cleaned = cleaned.replaceAll("\\s+", " ");

				if (!cleaned.isEmpty()) {

					// Take first 4–6 words (stable grouping)
					String[] words = cleaned.split(" ");
					int limit = Math.min(words.length, 5);

					return String.join(" ", Arrays.copyOfRange(words, 0, limit));
				}
			}
		}

		// 3️ Final fallback (never return Unknown)
		return "GenericApplicationError";
	}

	// grouping the error based on extract key
	public Map<String, List<String>> groupErrors(List<String> errorBlocks) {

		return errorBlocks.stream().collect(Collectors.groupingBy(this::extractKey));
	}

	// send the group error to AI
	public List<LogAnalysisResponse> analyzeGrouped(List<String> errorBlocks) {

		Map<String, List<String>> groupedErrors = groupErrors(errorBlocks);

		// 🔥 Find top issue (max occurrences)
		int maxCount = groupedErrors.values().stream().mapToInt(List::size).max().orElse(0);

		List<LogAnalysisResponse> result = new ArrayList<>();

		for (Map.Entry<String, List<String>> entry : groupedErrors.entrySet()) {

			String key = entry.getKey();
			List<String> logs = entry.getValue();

			int count = logs.size();
			boolean isTopIssue = count == maxCount;

			String sampleLog = logs.get(0);

			LogAnalysisResponse aiResponse = analyzeSingle(sampleLog);

			// 🔥 Create new response with count + top flag
			LogAnalysisResponse finalResponse = new LogAnalysisResponse(aiResponse.errorType(), count, isTopIssue,
					aiResponse.rootCause(), aiResponse.severity(), aiResponse.className(), aiResponse.methodName(),
					aiResponse.lineNumber(), aiResponse.faultyLine(), aiResponse.suggestedFix(),
					aiResponse.exampleSolution()

			);

			result.add(finalResponse);
		}

		return result;
	}

	private LogAnalysisResponse parseManually(String json) {
		try {
			// 🔥 CLEAN AI RESPONSE
			json = json.replaceAll("(?s)```.*?```", "") // remove any code block
					.replace("\\u003C", "<").replace("\\u003E", ">").trim();

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readValue(json, LogAnalysisResponse.class);
		} catch (Exception e) {
			e.printStackTrace();

			return new LogAnalysisResponse("Unknown", // errorType
					0, // occurrences
					false, // topIssue
					"Parsing failed", // rootCause
					"Low", // severity
					"Unknown", // className
					"Unknown", // methodName
					0, // lineNumber
					"Unknown", // faultyLine
					"Check AI output format", // suggestedFix
					json // exampleSolution (keep raw)
			);
		}
	}
	
	private String cleanWithJsoup(String html) {

	    Document doc = Jsoup.parseBodyFragment(html);

	    // 🔥 Important: convert to XML-safe output
	    doc.outputSettings()
	        .syntax(Document.OutputSettings.Syntax.xml)
	        .escapeMode(Entities.EscapeMode.xhtml);

	    return doc.body().html();
	}
	
}
