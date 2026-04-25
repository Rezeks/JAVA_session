package com.securemsg.security;

import java.security.KeyPair;

/**
 * Абстракция аппаратного модуля хранения ключей (HSM/TPM).
 */
public interface KeyVault {
    KeyPair getOrCreateSigningKeyPair(String ownerId);

    byte[] getOrCreateEncryptionKey(String ownerId);
}
