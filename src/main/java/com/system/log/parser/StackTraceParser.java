package com.system.log.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class StackTraceParser {

    private static final Pattern PATTERN = Pattern.compile(
            "at\\s+([a-zA-Z0-9_.]+)\\.([a-zA-Z0-9_]+)\\(([^:]+):(\\d+)\\)"
    );

    public StackTraceInfo parse(String log) {

        Matcher matcher = PATTERN.matcher(log);

        while (matcher.find()) {

            String className = matcher.group(1);
            String methodName = matcher.group(2);
            String fileName = matcher.group(3);
            int lineNumber = Integer.parseInt(matcher.group(4));

            // ✅ Skip framework classes (important)
            if (!className.startsWith("com.system")) {
                continue;
            }

            return new StackTraceInfo(className, methodName, fileName, lineNumber);
        }

        return null;
    }
}
