package com.securemsg.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {
    private final CryptoService cryptoService = new CryptoService();

    @Test
    void shouldEncryptAndDecrypt() {
        byte[] key = CryptoService.generateRandomBytes(32);
        String plain = "secret message";

        String encrypted = cryptoService.encrypt(plain, key);
        String decrypted = cryptoService.decrypt(encrypted, key);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    void shouldSignAndVerify() {
        InMemoryKeyVault keyVault = new InMemoryKeyVault();
        KeyPair keyPair = keyVault.getOrCreateSigningKeyPair("alice");
        String payload = "payload";

        String signature = cryptoService.sign(payload, keyPair.getPrivate());

        assertTrue(cryptoService.verify(payload, signature, keyPair.getPublic()));
        assertFalse(cryptoService.verify(payload + "tampered", signature, keyPair.getPublic()));
    }
}
