package com.securemsg.security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class CryptoService {
    private static final String AES_ALGO = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_SIZE = 12;

    public String encrypt(String plainText, byte[] key) {
        try {
            byte[] iv = generateRandomBytes(IV_SIZE);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, AES_ALGO), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer combined = ByteBuffer.allocate(iv.length + cipherText.length);
            combined.put(iv);
            combined.put(cipherText);
            return Base64.getEncoder().encodeToString(combined.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt message", e);
        }
    }

    public String decrypt(String encodedPayload, byte[] key) {
        try {
            byte[] raw = Base64.getDecoder().decode(encodedPayload);
            ByteBuffer combined = ByteBuffer.wrap(raw);
            byte[] iv = new byte[IV_SIZE];
            combined.get(iv);
            byte[] cipherText = new byte[combined.remaining()];
            combined.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, AES_ALGO), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt message", e);
        }
    }

    public String sign(String payload, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign payload", e);
        }
    }

    public boolean verify(String payload, String encodedSignature, PublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(encodedSignature));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to verify payload", e);
        }
    }

    public String wrapKey(byte[] key, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(key));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to wrap key", e);
        }
    }

    public byte[] unwrapKey(String wrappedKey, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(Base64.getDecoder().decode(wrappedKey));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to unwrap key", e);
        }
    }

    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public void encryptStream(InputStream source, OutputStream destination, byte[] key) {
        try {
            byte[] iv = generateRandomBytes(IV_SIZE);
            destination.write(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, AES_ALGO), new GCMParameterSpec(GCM_TAG_BITS, iv));
            try (CipherOutputStream encrypted = new CipherOutputStream(destination, cipher)) {
                source.transferTo(encrypted);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to encrypt stream", e);
        }
    }

    public void decryptStream(InputStream source, OutputStream destination, byte[] key) {
        try {
            byte[] iv = source.readNBytes(IV_SIZE);
            if (iv.length != IV_SIZE) {
                throw new IllegalStateException("Invalid encrypted stream header");
            }
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, AES_ALGO), new GCMParameterSpec(GCM_TAG_BITS, iv));
            try (CipherInputStream decrypted = new CipherInputStream(source, cipher)) {
                decrypted.transferTo(destination);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to decrypt stream", e);
        }
    }
}
