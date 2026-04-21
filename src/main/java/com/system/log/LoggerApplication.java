package com.system.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoggerApplication{
	
	 public static void main(String[] args) {
	        SpringApplication.run(LoggerApplication.class, args);
	        System.out.println("TrustStore: " + System.getProperty("javax.net.ssl.trustStore"));
	    }
}