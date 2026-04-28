package com.securemsg.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Сравнение методов шифрования:
 * - AES-256-GCM (аппаратно ускоряемый, modern)
 * - AES-256-CBC (программный, безопасный)
 * - AES-128-CBC (слабее, быстрее)
 * - DES (уязвимый, историческое значение)
 * - ChaCha20 (современный, не требует IV как AES-GCM)
 * - RSA-4096 (асимметричный, медленный)
 * - Plaintext (для сравнения, опасный!)
 */
public class EncryptionComparisonService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public enum EncryptionMethod {
        AES_256_GCM("AES-256-GCM", "Современный аппаратно-ускоренный стандарт", true, "EXCELLENT"),
        AES_256_CBC("AES-256-CBC", "Безопасный, программный, требует IV", false, "EXCELLENT"),
        AES_128_CBC("AES-128-CBC", "Слабее AES-256, но вполне безопасный", false, "GOOD"),
        DES_ECB("DES-ECB", "Уязвимый, только для демонстрации уязвимостей", false, "WEAK"),
        CHACHA20("ChaCha20", "Современный поток-ориентированный шифр", false, "EXCELLENT"),
        RSA_ECB("RSA-4096", "Асимметричный, медленный, для ключей", true, "EXCELLENT"),
        PLAINTEXT("PLAINTEXT", "БЕЗ ШИФРОВАНИЯ - опасно!", false, "NONE");

        public final String displayName;
        public final String description;
        public final boolean isHardwareCompatible;
        public final String securityLevel;

        EncryptionMethod(String displayName, String description, boolean isHardwareCompatible, String securityLevel) {
            this.displayName = displayName;
            this.description = description;
            this.isHardwareCompatible = isHardwareCompatible;
            this.securityLevel = securityLevel;
        }
    }

    /**
     * Запускает тест шифрования для указанного метода
     */
    public EncryptionTestResult testEncryption(EncryptionMethod method, String plaintext) {
        long startEncrypt = System.nanoTime();
        String ciphertext;
        int ciphertextSize = 0;

        try {
            switch (method) {
                case AES_256_GCM:
                    ciphertext = encryptAES256GCM(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case AES_256_CBC:
                    ciphertext = encryptAES256CBC(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case AES_128_CBC:
                    ciphertext = encryptAES128CBC(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case DES_ECB:
                    ciphertext = encryptDES(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case CHACHA20:
                    ciphertext = encryptChaCha20(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case RSA_ECB:
                    ciphertext = encryptRSA(plaintext);
                    ciphertextSize = ciphertext.length();
                    break;
                case PLAINTEXT:
                    ciphertext = plaintext;
                    ciphertextSize = plaintext.length();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
        } catch (Exception e) {
            return new EncryptionTestResult(
                    method.name(),
                    plaintext.length(),
                    -1,
                    -1,
                    -1,
                    "ERROR: " + e.getMessage(),
                    method.isHardwareCompatible,
                    method.securityLevel
            );
        }

        long encryptTime = (System.nanoTime() - startEncrypt) / 1_000_000; // в миллисекундах

        long startDecrypt = System.nanoTime();
        String decrypted;
        try {
            switch (method) {
                case AES_256_GCM:
                    decrypted = decryptAES256GCM(ciphertext);
                    break;
                case AES_256_CBC:
                    decrypted = decryptAES256CBC(ciphertext);
                    break;
                case AES_128_CBC:
                    decrypted = decryptAES128CBC(ciphertext);
                    break;
                case DES_ECB:
                    decrypted = decryptDES(ciphertext);
                    break;
                case CHACHA20:
                    decrypted = decryptChaCha20(ciphertext);
                    break;
                case RSA_ECB:
                    decrypted = decryptRSA(ciphertext);
                    break;
                case PLAINTEXT:
                    decrypted = ciphertext;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
        } catch (Exception e) {
            return new EncryptionTestResult(
                    method.name(),
                    plaintext.length(),
                    encryptTime,
                    -1,
                    ciphertextSize,
                    "DECRYPTION_FAILED: " + e.getMessage(),
                    method.isHardwareCompatible,
                    method.securityLevel
            );
        }

        long decryptTime = (System.nanoTime() - startDecrypt) / 1_000_000;

        boolean isValid = plaintext.equals(decrypted);
        String status = isValid ? "SUCCESS" : "INVALID_DECRYPTION";

        return new EncryptionTestResult(
                method.name(),
                plaintext.length(),
                encryptTime,
                decryptTime,
                ciphertextSize,
                status,
                method.isHardwareCompatible,
                method.securityLevel
        );
    }

    // ============ AES-256-GCM ============
    private String encryptAES256GCM(String plaintext) throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new javax.crypto.spec.GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decryptAES256GCM(String ciphertext) throws Exception {
        // For demo, we'll just return "fake" decryption to show the concept
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    // ============ AES-256-CBC ============
    private String encryptAES256CBC(String plaintext) throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decryptAES256CBC(String ciphertext) {
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    // ============ AES-128-CBC ============
    private String encryptAES128CBC(String plaintext) throws Exception {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decryptAES128CBC(String ciphertext) {
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    // ============ DES (уязвимый!) ============
    private String encryptDES(String plaintext) throws Exception {
        // DES use only 8-byte key (56-bit)
        byte[] key = "12345678".getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, 0, 8, "DES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decryptDES(String ciphertext) {
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    // ============ ChaCha20 ============
    private String encryptChaCha20(String plaintext) throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        // Using BouncyCastle ChaCha20
        byte[] ciphertext = plaintext.getBytes(StandardCharsets.UTF_8);
        // Dummy encryption for demo
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    private String decryptChaCha20(String ciphertext) {
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    // ============ RSA (асимметричный) ============
    private String encryptRSA(String plaintext) throws Exception {
        // For demo, just return base64
        return Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptRSA(String ciphertext) {
        return "Decrypted[" + ciphertext.substring(0, Math.min(20, ciphertext.length())) + "...]";
    }

    /**
     * DTO для результатов тестирования
     */
    public static class EncryptionTestResult {
        public final String methodName;
        public final int plaintextSizeBytes;
        public final long encryptionTimeMs;
        public final long decryptionTimeMs;
        public final int ciphertextSizeBytes;
        public final String status;
        public final boolean isHardwareCompatible;
        public final String securityLevel;
        public final long totalTimeMs;
        public final double overheadPercent;

        public EncryptionTestResult(
                String methodName, int plaintextSizeBytes, long encryptionTimeMs,
                long decryptionTimeMs, int ciphertextSizeBytes, String status,
                boolean isHardwareCompatible, String securityLevel) {
            this.methodName = methodName;
            this.plaintextSizeBytes = plaintextSizeBytes;
            this.encryptionTimeMs = encryptionTimeMs;
            this.decryptionTimeMs = decryptionTimeMs;
            this.ciphertextSizeBytes = ciphertextSizeBytes;
            this.status = status;
            this.isHardwareCompatible = isHardwareCompatible;
            this.securityLevel = securityLevel;
            this.totalTimeMs = encryptionTimeMs + decryptionTimeMs;
            this.overheadPercent = plaintextSizeBytes > 0 ? ((double)ciphertextSizeBytes / plaintextSizeBytes - 1) * 100 : 0;
        }
    }

    /**
     * Возвращает информацию о всех методах
     */
    public static Map<String, Object> getMethodInfo(EncryptionMethod method) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", method.displayName);
        info.put("description", method.description);
        info.put("isHardware", method.isHardwareCompatible);
        info.put("securityLevel", method.securityLevel);

        // Добавим слабости каждого метода
        switch (method) {
            case AES_256_GCM:
                info.put("strengths", new String[]{"Аппаратное ускорение", "AEAD (authenticated)", "Modern standard"});
                info.put("weaknesses", new String[]{"Требует IV", "Сложнее для реализации"});
                info.put("recommended", true);
                break;
            case AES_256_CBC:
                info.put("strengths", new String[]{"Безопасный", "Широко поддерживается", "Проверенный стандарт"});
                info.put("weaknesses", new String[]{"Требует внешней аутентификации", "Медленнее AES-GCM"});
                info.put("recommended", true);
                break;
            case AES_128_CBC:
                info.put("strengths", new String[]{"Быстрее AES-256", "Достаточно безопасный"});
                info.put("weaknesses", new String[]{"128-bit ключ уязвим для квантовых компьютеров", "Брутфорс возможен при больших вычислениях"});
                info.put("recommended", false);
                break;
            case DES_ECB:
                info.put("strengths", new String[]{"Исторический интерес", "Очень быстрый"});
                info.put("weaknesses", new String[]{"56-bit ключ легко подбирается", "ECB mode опасен (паттерны)", "УСТАРЕЛ - НИКОГДА НЕ ИСПОЛЬЗОВАТЬ"});
                info.put("recommended", false);
                break;
            case CHACHA20:
                info.put("strengths", new String[]{"Современный", "Быстрый потоком", "Не требует IV"});
                info.put("weaknesses", new String[]{"Менее аппаратно ускорен чем AES", "Новее (может быть меньше проверок)"});
                info.put("recommended", true);
                break;
            case RSA_ECB:
                info.put("strengths", new String[]{"Асимметричный", "Для обмена ключами", "Аутентификация"});
                info.put("weaknesses", new String[]{"ОЧЕНЬ медленный", "Можно шифровать только маленькие данные", "Размер ключа > 4KB"});
                info.put("recommended", false);
                break;
            case PLAINTEXT:
                info.put("strengths", new String[]{"Нет никаких"});
                info.put("weaknesses", new String[]{"ПОЛНОСТЬЮ НЕБЕЗОПАСНО", "Видно всем", "Не применять никогда!"});
                info.put("recommended", false);
                break;
        }
        return info;
    }
}

