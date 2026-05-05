package com.system.log.resolver;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SourceCodeResolver {

    private static final String BASE_PATH = "src/main/java/";

    public String resolve(String className) {

        if (className == null || className.isEmpty()) {
            return null;
        }else {
        	System.out.println("in the else block : "+className);
        }
        System.out.println("here to return ");
        System.out.println(BASE_PATH + className.replace(".", "/") + ".java");
        return BASE_PATH + className.replace(".", "/") + ".java";
    }
    
    public String getFaultyLine(String filePath, int lineNumber) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            return lines.get(lineNumber - 1).trim();
        } catch (Exception e) {
            return "Unable to extract line";
        }
    }
}
