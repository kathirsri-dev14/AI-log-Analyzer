package com.system.log.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.system.log.dto.LogAnalysisResponse;

@Service
public class LogAnalyzerService {
	
	 private ChatClient chatClient;
	 

	    public LogAnalyzerService(ChatClient.Builder builder) {
	        this.chatClient = builder.build();
	    }

	    public String analyzeLogs(String logs) {
	    	
	    	String prompt = buildPrompt(logs);
	    	 return chatClient.prompt()
	                .user(prompt)
	                .call()
	                .content();
	    }
	    
	    public List<LogAnalysisResponse> analyzeMultiple(List<String> logsList) {

	        return logsList.stream()
	                .map(this::analyzeSingle)
	                .toList();
	    }
	    
	    private LogAnalysisResponse analyzeSingle(String logs) {

	        String prompt = buildPrompt(logs);

	        return chatClient.prompt()
	                .user(prompt)
	                .call()
	                .entity(LogAnalysisResponse.class);
	    }
	    
	    private String buildPrompt(String logs) {

	        return """
	        You are a senior production support engineer analyzing application logs.

	        Analyze the log and return ONLY a valid JSON object.

	        STRICT OUTPUT RULES:
	        - Output MUST be valid JSON
	        - Do NOT include markdown, comments, or extra text
	        - Do NOT wrap JSON in code blocks
	        - Use the exact field names provided
	        - severity must be one of: Low, Medium, High, Critical
	        - If information is unknown, use "Unknown"
	        - Keep responses concise and practical

	        Required JSON format:

	        {{
	          "errorType": "",
	          "rootCause": "",
	          "severity": "Low | Medium | High | Critical",
	          "suggestedFix": "",
	          "exampleSolution": ""
	        }}
	        
	        errorType rules:
	        - If the log contains an Exception (e.g., NullPointerException, ArithmeticException), extract it EXACTLY
	        - If no explicit exception is present, infer the errorType from the message
	        - NEVER return "Unknown" if a reasonable inference can be made

	        For exampleSolution:
	        - Provide a short code snippet or clear actionable steps
	        - Use \\n for line breaks
	        - Keep under 10 lines

	        Log:
	        """ + logs;
	    }
	    //extract key to find the error
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
	    
	    //grouping the error based on extract key
	    public Map<String, List<String>> groupErrors(List<String> errorBlocks) {

	        return errorBlocks.stream()
	                .collect(Collectors.groupingBy(this::extractKey));
	    }
	    //send the group error to AI 
	    public List<LogAnalysisResponse> analyzeGrouped(List<String> errorBlocks) {

	        Map<String, List<String>> groupedErrors = groupErrors(errorBlocks);

	        // 🔥 Find top issue (max occurrences)
	        int maxCount = groupedErrors.values().stream()
	                .mapToInt(List::size)
	                .max()
	                .orElse(0);

	        List<LogAnalysisResponse> result = new ArrayList<>();

	        for (Map.Entry<String, List<String>> entry : groupedErrors.entrySet()) {

	            String key = entry.getKey();
	            List<String> logs = entry.getValue();

	            int count = logs.size();
	            boolean isTopIssue = count == maxCount;

	            String sampleLog = logs.get(0);

	            LogAnalysisResponse aiResponse = analyzeSingle(sampleLog);

	            // 🔥 Create new response with count + top flag
	            LogAnalysisResponse finalResponse = new LogAnalysisResponse(
	                    aiResponse.errorType(),
	                    count,
	                    isTopIssue,
	                    aiResponse.rootCause(),
	                    aiResponse.severity(),
	                    aiResponse.issueCategory(),
	                    aiResponse.incidentSummary(),
	                    aiResponse.suggestedFix(),
	                    aiResponse.exampleSolution()
	                   
	            );

	            result.add(finalResponse);
	        }

	        return result;
	    }
}
