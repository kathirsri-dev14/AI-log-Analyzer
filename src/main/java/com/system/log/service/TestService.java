package com.system.log.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class TestService {

    // 1️⃣ NullPointerException
    public void nullPointerScenario() {
        String name = null;
        System.out.println(name.length()); // NPE
    }

    // 2️⃣ ArithmeticException
    public void divideByZero() {
        int result = 10 / 0; // ArithmeticException
    }

    // 3️⃣ IndexOutOfBoundsException
    public void listIndexError() {
        List<String> list = List.of("A", "B");
        System.out.println(list.get(5)); // Index error
    }

    // 4️⃣ NumberFormatException
    public void numberFormatError() {
        int value = Integer.parseInt("ABC"); // Invalid number
    }

    // 5️⃣ IllegalArgumentException (realistic validation)
    public void validateAge(int age) {
        if (age < 18) {
            throw new IllegalArgumentException("Age must be >= 18");
        }
    }

    // 6️⃣ Simulated DB error (very realistic)
    public void databaseSimulation() {
        throw new RuntimeException("Database connection timeout");
    }

    // 7️⃣ Map access error
    public void mapAccessError() {
        Map<String, String> map = Map.of("key1", "value1");
        System.out.println(map.get("key2").toString()); // NPE
    }
}