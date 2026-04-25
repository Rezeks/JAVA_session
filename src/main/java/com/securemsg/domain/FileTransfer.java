package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record FileTransfer(
        UUID id,
        UUID senderId,
        UUID recipientId,
        String originalFileName,
        long totalSize,
        long uploadedBytes,
        String encryptedPath,
        String checksumSha256,
        FileTransferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public FileTransfer withUploadedBytes(long bytes) {
        return new FileTransfer(id, senderId, recipientId, originalFileName, totalSize, bytes, encryptedPath, checksumSha256, status, createdAt, Instant.now());
    }

    public FileTransfer withEncryptedPath(String path, String checksum, FileTransferStatus newStatus) {
        return new FileTransfer(id, senderId, recipientId, originalFileName, totalSize, uploadedBytes, path, checksum, newStatus, createdAt, Instant.now());
    }

    public FileTransfer withStatus(FileTransferStatus newStatus) {
        return new FileTransfer(id, senderId, recipientId, originalFileName, totalSize, uploadedBytes, encryptedPath, checksumSha256, newStatus, createdAt, Instant.now());
    }
}

