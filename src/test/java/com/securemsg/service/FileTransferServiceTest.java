package com.securemsg.service;

import com.securemsg.domain.FileTransfer;
import com.securemsg.domain.FileTransferStatus;
import com.securemsg.security.CryptoService;
import com.securemsg.security.InMemoryKeyVault;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTransferServiceTest {

    @Test
    void shouldUploadFinalizeAndVerifyChecksum() {
        AuditService audit = new AuditService();
        InMemoryKeyVault keyVault = new InMemoryKeyVault();
        MessagingService messaging = new MessagingService(new CryptoService(), keyVault, audit);
        FileTransferService files = new FileTransferService(new CryptoService(), keyVault, messaging, audit, Path.of("target", "test-storage"));

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        byte[] content = "hello-file".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        FileTransfer started = files.initiateUpload(sender, recipient, "hello.txt", content.length);
        files.uploadChunk(started.id(), 0, new ByteArrayInputStream(content));
        FileTransfer stored = files.finalizeUpload(started.id());

        assertEquals(FileTransferStatus.STORED, stored.status());
        assertTrue(files.verifyChecksum(stored.id(), stored.checksumSha256()));
    }
}

