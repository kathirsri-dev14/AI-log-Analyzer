package com.system.log.extractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MethodExtractor {

    public String extractMethod(String filePath, int errorLine) {

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            int index = errorLine - 1;

            // 🔼 Find method start
            int start = index;
            while (start > 0 && !lines.get(start).contains("{")) {
                start--;
            }

            // 🔽 Find method end
            int end = start;
            int braceCount = 0;

            for (int i = start; i < lines.size(); i++) {
                String line = lines.get(i);

                if (line.contains("{")) {
					braceCount++;
				}
                if (line.contains("}")) {
					braceCount--;
				}

                if (braceCount == 0) {
                    end = i;
                    break;
                }
            }

            return String.join("\n", lines.subList(start, end + 1));

        } catch (IOException e) {
            return "Unable to read source file";
        }
    }
}