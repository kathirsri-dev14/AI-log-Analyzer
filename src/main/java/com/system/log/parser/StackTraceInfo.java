package com.system.log.parser;

public record StackTraceInfo(
        String className,
        String methodName,
        String fileName,
        int lineNumber
) {
	public boolean isValid() {
        return className != null && lineNumber > 0;
    }
}

