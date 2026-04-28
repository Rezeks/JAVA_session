package com.securemsg.api;

import com.securemsg.security.EncryptionComparisonService;
import com.securemsg.security.EncryptionComparisonService.EncryptionMethod;
import com.securemsg.security.EncryptionComparisonService.EncryptionTestResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/encryption")
public class EncryptionComparisonController {

    private final EncryptionComparisonService encryptionService = new EncryptionComparisonService();

    /**
     * Тестирует все методы шифрования на одинаковом входе
     */
    @PostMapping("/test-all")
    public Map<String, Object> testAllMethods(@RequestParam(defaultValue = "Hello confidential world! This is a test message for encryption comparison.") String plaintext) {
        List<EncryptionTestResult> results = new ArrayList<>();

        for (EncryptionMethod method : EncryptionMethod.values()) {
            EncryptionTestResult result = encryptionService.testEncryption(method, plaintext);
            results.add(result);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("plaintext", plaintext);
        response.put("plaintextSize", plaintext.length());
        response.put("timestamp", System.currentTimeMillis());
        response.put("results", results);
        response.put("summary", generateSummary(results));

        return response;
    }

    /**
     * Тестирует конкретный метод шифрования
     */
    @PostMapping("/test/{method}")
    public Map<String, Object> testSingleMethod(
            @PathVariable String method,
            @RequestParam(defaultValue = "Test message for encryption") String plaintext) {

        try {
            EncryptionMethod encMethod = EncryptionMethod.valueOf(method.toUpperCase());
            EncryptionTestResult result = encryptionService.testEncryption(encMethod, plaintext);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("plaintext", plaintext);
            response.put("method", encMethod.displayName);
            response.put("result", result);
            response.put("info", EncryptionComparisonService.getMethodInfo(encMethod));

            return response;
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Unknown method: " + method);
        }
    }

    /**
     * Возвращает информацию о конкретном методе
     */
    @GetMapping("/info/{method}")
    public Map<String, Object> getMethodInfo(@PathVariable String method) {
        try {
            EncryptionMethod encMethod = EncryptionMethod.valueOf(method.toUpperCase());
            return EncryptionComparisonService.getMethodInfo(encMethod);
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Unknown method: " + method);
        }
    }

    /**
     * Возвращает список всех доступных методов
     */
    @GetMapping("/methods")
    public List<Map<String, Object>> getAllMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();

        for (EncryptionMethod method : EncryptionMethod.values()) {
            Map<String, Object> methodInfo = new LinkedHashMap<>();
            methodInfo.put("name", method.name());
            methodInfo.put("displayName", method.displayName);
            methodInfo.put("description", method.description);
            methodInfo.put("isHardwareCompatible", method.isHardwareCompatible);
            methodInfo.put("securityLevel", method.securityLevel);

            // Добавляем из getMethodInfo
            Map<String, Object> fullInfo = EncryptionComparisonService.getMethodInfo(method);
            methodInfo.putAll(fullInfo);

            methods.add(methodInfo);
        }

        return methods;
    }

    /**
     * Сравнение двух методов
     */
    @PostMapping("/compare")
    public Map<String, Object> compareMethods(
            @RequestParam String method1,
            @RequestParam String method2,
            @RequestParam(defaultValue = "Test message for comparison") String plaintext) {

        try {
            EncryptionMethod m1 = EncryptionMethod.valueOf(method1.toUpperCase());
            EncryptionMethod m2 = EncryptionMethod.valueOf(method2.toUpperCase());

            EncryptionTestResult result1 = encryptionService.testEncryption(m1, plaintext);
            EncryptionTestResult result2 = encryptionService.testEncryption(m2, plaintext);

            Map<String, Object> comparison = new LinkedHashMap<>();
            comparison.put("method1", result1);
            comparison.put("method2", result2);
            comparison.put("comparison", Map.of(
                    "encryptionTimeDiff", result1.encryptionTimeMs - result2.encryptionTimeMs,
                    "decryptionTimeDiff", result1.decryptionTimeMs - result2.decryptionTimeMs,
                    "totalTimeDiff", result1.totalTimeMs - result2.totalTimeMs,
                    "ciphertextSizeDiff", result1.ciphertextSizeBytes - result2.ciphertextSizeBytes,
                    "faster", result1.totalTimeMs < result2.totalTimeMs ? "method1" : "method2",
                    "smaller", result1.ciphertextSizeBytes < result2.ciphertextSizeBytes ? "method1" : "method2"
            ));

            return comparison;
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Unknown method");
        }
    }

    /**
     * Генерирует сводку результатов
     */
    private Map<String, Object> generateSummary(List<EncryptionTestResult> results) {
        Map<String, Object> summary = new LinkedHashMap<>();

        // Найти самый быстрый метод
        EncryptionTestResult fastest = results.stream()
                .filter(r -> !r.status.contains("ERROR"))
                .min(Comparator.comparingLong(r -> r.totalTimeMs))
                .orElse(null);

        // Найти самый компактный метод
        EncryptionTestResult mostCompact = results.stream()
                .filter(r -> !r.status.contains("ERROR"))
                .min(Comparator.comparingInt(r -> r.ciphertextSizeBytes))
                .orElse(null);

        // Найти самый безопасный метод
        EncryptionTestResult mostSecure = results.stream()
                .filter(r -> "EXCELLENT".equals(r.securityLevel))
                .findFirst()
                .orElse(null);

        summary.put("fastest", fastest != null ? Map.of(
                "method", fastest.methodName,
                "time", fastest.totalTimeMs + "ms"
        ) : "No suitable method");

        summary.put("mostCompact", mostCompact != null ? Map.of(
                "method", mostCompact.methodName,
                "size", mostCompact.ciphertextSizeBytes + " bytes",
                "overhead", String.format("%.1f%%", mostCompact.overheadPercent)
        ) : "No suitable method");

        summary.put("mostSecure", mostSecure != null ? Map.of(
                "method", mostSecure.methodName,
                "level", mostSecure.securityLevel
        ) : "No secure method found");

        // Рекомендуемый метод (баланс скорости, безопасности и компактности)
        summary.put("recommended", "AES-256-GCM - лучший баланс между скоростью, безопасностью и поддержкой аппаратного ускорения");

        return summary;
    }
}

