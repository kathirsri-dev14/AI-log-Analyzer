package com.system.log.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class LogFileReaderService {

    public String readErrorLogs() throws IOException {

        Path path = Paths.get("logs/app.log");
        
        List<String> lines = Files.readAllLines(path);

        int fromIndex = Math.max(lines.size() - 200, 0);

        List<String> recentLines = lines.subList(fromIndex, lines.size());

        return recentLines.stream()
                .filter(line -> line.contains("ERROR") || line.contains("Exception"))
                .collect(Collectors.joining("\n"));
    }
    
    public List<String> extractErrorBlocks() throws IOException {

        Path path = Paths.get("logs/app.log");
        List<String> lines = Files.readAllLines(path);

        // read only last 500 lines (important for production)
        int fromIndex = Math.max(lines.size() - 500, 0);
        List<String> recentLogs = lines.subList(fromIndex, lines.size());

        List<String> errorBlocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();

        for (String line : recentLogs) {

            if (line.contains("ERROR")) {

                if (currentBlock.length() > 0) {
                    errorBlocks.add(currentBlock.toString());
                    currentBlock.setLength(0);
                }

                currentBlock.append(line).append("\n");

            } else if (currentBlock.length() > 0) {

            	 if (line.startsWith("\t")
                         || line.trim().startsWith("at ")
                         || line.contains("Exception")
                         || line.contains("Caused by")) {

                     currentBlock.append(line).append("\n");

                 } else {
                   
                     errorBlocks.add(currentBlock.toString());
                     currentBlock.setLength(0);
                 }
            }
        }

        if (currentBlock.length() > 0) {
            errorBlocks.add(currentBlock.toString());
        }

        return errorBlocks.stream()
                .limit(5) // limit AI calls
                .toList();
    }
}