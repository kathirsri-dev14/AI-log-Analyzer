package com.system.log.dto;

public record LogAnalysisResponse(
	    String errorType,
	    int occurrences,
	    boolean topIssue,
	    String rootCause,
	    String severity,
	    String issueCategory,
	    String incidentSummary,
	    String suggestedFix,
	    String exampleSolution
	) {}
