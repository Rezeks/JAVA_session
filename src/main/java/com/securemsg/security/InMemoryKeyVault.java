package com.securemsg.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKeyVault implements KeyVault {
    private final Map<String, KeyPair> signingKeys = new ConcurrentHashMap<>();
    private final Map<String, byte[]> encryptionKeys = new ConcurrentHashMap<>();

    @Override
    public KeyPair getOrCreateSigningKeyPair(String ownerId) {
        return signingKeys.computeIfAbsent(ownerId, ignored -> generateSigningKeyPair());
    }

    @Override
    public void rotateSigningKeyPair(String ownerId) {
        signingKeys.put(ownerId, generateSigningKeyPair());
    }

    @Override
    public byte[] getOrCreateEncryptionKey(String ownerId) {
        return encryptionKeys.computeIfAbsent(ownerId,
                ignored -> Base64.getDecoder().decode(Base64.getEncoder().encodeToString(CryptoService.generateRandomBytes(32))));
    }

    @Override
    public void rotateEncryptionKey(String ownerId) {
        encryptionKeys.put(ownerId, CryptoService.generateRandomBytes(32));
    }

    @Override
    public String exportPublicKey(String ownerId) {
        KeyPair pair = getOrCreateSigningKeyPair(ownerId);
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    @Override
    public String exportPrivateKey(String ownerId) {
        KeyPair pair = getOrCreateSigningKeyPair(ownerId);
        return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    }

    @Override
    public void importSigningKeyPair(String ownerId, String base64PublicKey, String base64PrivateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey)));
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey)));
            signingKeys.put(ownerId, new KeyPair(publicKey, privateKey));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to import key pair", e);
        }
    }

    private KeyPair generateSigningKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm is not available", e);
        }
    }
}
