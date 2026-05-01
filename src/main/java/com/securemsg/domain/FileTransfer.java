package com.securemsg.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_transfers")
public class FileTransfer {

    @Id
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "total_size", nullable = false)
    private long totalSize;

    @Column(name = "uploaded_bytes")
    private long uploadedBytes;

    @Column(name = "encrypted_path")
    private String encryptedPath;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileTransferStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected FileTransfer() {
    }

    public FileTransfer(UUID id, UUID senderId, UUID recipientId, String originalFileName,
                        long totalSize, long uploadedBytes, String encryptedPath, String checksumSha256,
                        FileTransferStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.originalFileName = originalFileName;
        this.totalSize = totalSize;
        this.uploadedBytes = uploadedBytes;
        this.encryptedPath = encryptedPath;
        this.checksumSha256 = checksumSha256;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters (record-style) ---
    public UUID id() { return id; }
    public UUID senderId() { return senderId; }
    public UUID recipientId() { return recipientId; }
    public String originalFileName() { return originalFileName; }
    public long totalSize() { return totalSize; }
    public long uploadedBytes() { return uploadedBytes; }
    public String encryptedPath() { return encryptedPath; }
    public String checksumSha256() { return checksumSha256; }
    public FileTransferStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    // --- JavaBean getters ---
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getRecipientId() { return recipientId; }
    public String getOriginalFileName() { return originalFileName; }
    public long getTotalSize() { return totalSize; }
    public long getUploadedBytes() { return uploadedBytes; }
    public String getEncryptedPath() { return encryptedPath; }
    public String getChecksumSha256() { return checksumSha256; }
    public FileTransferStatus getFileTransferStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Setters ---
    public void setUploadedBytes(long bytes) { this.uploadedBytes = bytes; this.updatedAt = Instant.now(); }
    public void setStatus(FileTransferStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void setEncryptedPath(String path) { this.encryptedPath = path; this.updatedAt = Instant.now(); }
    public void setChecksumSha256(String checksum) { this.checksumSha256 = checksum; this.updatedAt = Instant.now(); }

    // --- Legacy with* ---
    public FileTransfer withUploadedBytes(long bytes) { setUploadedBytes(bytes); return this; }

    public FileTransfer withEncryptedPath(String path, String checksum, FileTransferStatus newStatus) {
        this.encryptedPath = path;
        this.checksumSha256 = checksum;
        this.status = newStatus;
        this.updatedAt = Instant.now();
        return this;
    }

    public FileTransfer withStatus(FileTransferStatus newStatus) { setStatus(newStatus); return this; }
}
