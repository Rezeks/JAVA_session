package com.securemsg.service;

import com.securemsg.domain.FileTransfer;
import com.securemsg.domain.FileTransferStatus;
import com.securemsg.repository.FileTransferRepository;
import com.securemsg.security.CryptoService;
import com.securemsg.security.KeyVault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public class FileTransferService {
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    private final FileTransferRepository fileTransferRepository;
    private final CryptoService cryptoService;
    private final KeyVault keyVault;
    private final MessagingService messagingService;
    private final AuditService auditService;
    private final Path storageRoot;

    public FileTransferService(CryptoService cryptoService,
                               KeyVault keyVault,
                               MessagingService messagingService,
                               AuditService auditService,
                               Path storageRoot,
                               FileTransferRepository fileTransferRepository) {
        this.cryptoService = cryptoService;
        this.keyVault = keyVault;
        this.messagingService = messagingService;
        this.auditService = auditService;
        this.storageRoot = storageRoot;
        this.fileTransferRepository = fileTransferRepository;
    }

    public FileTransfer initiateUpload(UUID senderId, UUID recipientId, String fileName, long totalSize) {
        if (totalSize <= 0 || totalSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be in range (0, 2GB]");
        }
        UUID transferId = UUID.randomUUID();
        FileTransfer transfer = new FileTransfer(
                transferId,
                senderId,
                recipientId,
                fileName,
                totalSize,
                0,
                null,
                null,
                FileTransferStatus.UPLOADING,
                java.time.Instant.now(),
                java.time.Instant.now());
        fileTransferRepository.save(transfer);
        auditService.record("FILE_UPLOAD_INIT", senderId.toString(), "Transfer " + transferId + " started");
        return transfer;
    }

    public FileTransfer uploadChunk(UUID transferId, long offset, InputStream chunkData) {
        FileTransfer transfer = requireTransfer(transferId);
        if (transfer.status() != FileTransferStatus.UPLOADING) {
            throw new IllegalStateException("Transfer is not in UPLOADING state");
        }
        Path plainPath = plainUploadPath(transfer.id());
        try {
            Files.createDirectories(storageRoot);
            try (RandomAccessFile file = new RandomAccessFile(plainPath.toFile(), "rw")) {
                file.seek(offset);
                byte[] buffer = new byte[8192];
                int read;
                long written = 0;
                while ((read = chunkData.read(buffer)) != -1) {
                    file.write(buffer, 0, read);
                    written += read;
                }
                long uploaded = Math.max(transfer.uploadedBytes(), offset + written);
                transfer.withUploadedBytes(uploaded);
                fileTransferRepository.save(transfer);
                return transfer;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload chunk", e);
        }
    }

    public FileTransfer finalizeUpload(UUID transferId) {
        FileTransfer transfer = requireTransfer(transferId);
        Path plainPath = plainUploadPath(transfer.id());
        if (!Files.exists(plainPath)) {
            throw new IllegalStateException("No uploaded data found");
        }
        if (transfer.uploadedBytes() < transfer.totalSize()) {
            throw new IllegalStateException("Upload is incomplete");
        }

        Path encryptedPath = encryptedUploadPath(transfer.id());
        byte[] key = keyVault.getOrCreateEncryptionKey(transfer.recipientId().toString());
        try (InputStream source = Files.newInputStream(plainPath);
             OutputStream destination = Files.newOutputStream(encryptedPath)) {
            cryptoService.encryptStream(source, destination, key);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encrypt uploaded file", e);
        }

        String checksum = checksumSha256(plainPath);
        transfer.withEncryptedPath(encryptedPath.toString(), checksum, FileTransferStatus.STORED);
        fileTransferRepository.save(transfer);

        String payload = "file:" + transfer.id() + ":" + transfer.originalFileName() + ":" + checksum;
        messagingService.sendFileNotification(transfer.senderId(), transfer.recipientId(), transfer.id(), payload);
        auditService.record("FILE_UPLOAD_FINALIZED", transfer.senderId().toString(), "Transfer " + transfer.id() + " finalized");
        return transfer;
    }

    public boolean verifyChecksum(UUID transferId, String expectedChecksum) {
        FileTransfer transfer = requireTransfer(transferId);
        return transfer.checksumSha256() != null && transfer.checksumSha256().equalsIgnoreCase(expectedChecksum);
    }

    public void confirmDelivered(UUID transferId) {
        FileTransfer transfer = requireTransfer(transferId);
        transfer.withStatus(FileTransferStatus.DELIVERED);
        fileTransferRepository.save(transfer);
        auditService.record("FILE_DELIVERED", transfer.recipientId().toString(), "Transfer " + transfer.id() + " delivered");
    }

    private FileTransfer requireTransfer(UUID transferId) {
        return fileTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
    }

    private Path plainUploadPath(UUID transferId) {
        return storageRoot.resolve(transferId + ".upload");
    }

    private Path encryptedUploadPath(UUID transferId) {
        return storageRoot.resolve(transferId + ".enc");
    }

    private String checksumSha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Failed to calculate checksum", e);
        }
    }
}
