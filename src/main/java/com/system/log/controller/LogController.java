package com.system.log.controller;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.log.service.LogFileReaderService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("Logging")
public class LogController {
	private static final Logger log = LoggerFactory.getLogger(LogController.class);

    @Autowired
    private LogFileReaderService logFileReaderService;

    private final Random random = new Random();

  
    @GetMapping("/success")
    public String success() {
        log.info("User login successful for userId=123");
        return "Success log generated";
    }

    @GetMapping("/warning")
    public String warning() {
        log.warn("Payment processing delayed for orderId=456");
        return "Warning log generated";
    }

    //Arithmetic Exception
    @GetMapping("/error")
    public String error() {
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing request", e);
        }
        return "ArithmeticException log generated";
    }

    //NullPointerException
    @SuppressWarnings("null")
	@GetMapping("/npe")
    public String nullPointer() {
        try {
            String value = null;
            value.length();
        } catch (Exception e) {
            log.error("Null pointer while accessing user data", e);
        }
        return "NPE log generated";
    }

    //Database Error
    @GetMapping("/db-error")
    public String dbError() {
        try {
            throw new RuntimeException("DatabaseConnectionException");
        } catch (Exception e) {
            log.error("Database connection failure", e);
        }
        return "Database error log generated";
    }

    @GetMapping("/api-timeout")
    public String apiTimeout() {
        try {
            throw new RuntimeException("ApiTimeoutException");
        } catch (Exception e) {
            log.error("External API timeout", e);
        }
        return "API timeout log generated";
    }

    //Validation Error
    @GetMapping("/validation-error")
    public String validationError() {
        try {
            throw new IllegalArgumentException("Invalid input: userId cannot be null");
        } catch (Exception e) {
            log.error("Validation failed for incoming request", e);
        }
        return "Validation error log generated";
    }

    @GetMapping("/json-error")
    public String jsonError() {
        try {
            throw new RuntimeException("JsonParsingException");
        } catch (Exception e) {
            log.error("Error parsing JSON request", e);
        }
        return "JSON error log generated";
    }

    //Random Realistic Errors
    @GetMapping("/random-errors")
    public String randomErrors() {

        int type = random.nextInt(5);

        switch (type) {

            case 0 -> log.error("Database timeout while fetching customer data",
                    new RuntimeException("DatabaseTimeoutException"));

            case 1 -> log.error("Null pointer in payment service",
                    new NullPointerException("PaymentObjectNullException"));

            case 2 -> log.error("External API failed",
                    new RuntimeException("ApiFailureException"));

            case 3 -> log.error("Validation error",
                    new IllegalArgumentException("ValidationException"));

            case 4 -> log.error("Cache failure",
                    new RuntimeException("CacheConnectionException"));
        }

        return "Random production-like error generated";
    }
    
    // 📄 Read Logs
    @GetMapping("/readLogs")
    public List<String> readLogs() throws IOException {
        return logFileReaderService.extractErrorBlocks();
    }

}
