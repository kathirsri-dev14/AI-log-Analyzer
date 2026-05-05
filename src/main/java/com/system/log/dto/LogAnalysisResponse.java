package com.system.log.dto;

public record LogAnalysisResponse(
	    String errorType,
	    int occurrences,
	    boolean topIssue,
	    String rootCause,
	    String severity,
	    String className,
	    String methodName,
	    int lineNumber,
	    String faultyLine,
	    String suggestedFix,
	    String exampleSolution
	) {}
