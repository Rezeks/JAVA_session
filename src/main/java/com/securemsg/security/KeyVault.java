package com.securemsg.security;

import java.security.KeyPair;

/**
 * Абстракция аппаратного модуля хранения ключей (HSM/TPM).
 */
public interface KeyVault {
    KeyPair getOrCreateSigningKeyPair(String ownerId);

    void rotateSigningKeyPair(String ownerId);

    byte[] getOrCreateEncryptionKey(String ownerId);

    void rotateEncryptionKey(String ownerId);

    String exportPublicKey(String ownerId);

    String exportPrivateKey(String ownerId);

    void importSigningKeyPair(String ownerId, String base64PublicKey, String base64PrivateKey);
}
