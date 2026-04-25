package com.securemsg.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKeyVault implements KeyVault {
    private final Map<String, KeyPair> signingKeys = new ConcurrentHashMap<>();
    private final Map<String, byte[]> encryptionKeys = new ConcurrentHashMap<>();

    @Override
    public KeyPair getOrCreateSigningKeyPair(String ownerId) {
        return signingKeys.computeIfAbsent(ownerId, ignored -> {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(3072);
                return generator.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("RSA algorithm is not available", e);
            }
        });
    }

    @Override
    public byte[] getOrCreateEncryptionKey(String ownerId) {
        return encryptionKeys.computeIfAbsent(ownerId,
                ignored -> Base64.getDecoder().decode(Base64.getEncoder().encodeToString(CryptoService.generateRandomBytes(32))));
    }
}
