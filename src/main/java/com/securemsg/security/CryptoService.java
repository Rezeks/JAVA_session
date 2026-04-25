package com.securemsg.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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

    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
