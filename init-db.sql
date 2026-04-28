-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    hardware_token_secret VARCHAR(255),
    failed_auth_attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    group_id UUID,
    encrypted_payload TEXT NOT NULL,
    wrapped_message_key TEXT NOT NULL,
    ratchet_step INTEGER,
    signature VARCHAR(1024) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- Group chats table
CREATE TABLE group_chats (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Group members table
CREATE TABLE group_members (
    group_id UUID NOT NULL,
    member_id UUID NOT NULL,
    PRIMARY KEY (group_id, member_id),
    FOREIGN KEY (group_id) REFERENCES group_chats(id),
    FOREIGN KEY (member_id) REFERENCES users(id)
);

-- File transfers table
CREATE TABLE file_transfers (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    message_key VARCHAR(1024) NOT NULL,
    checksum VARCHAR(64),
    delivery_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- Audit events table
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    actor VARCHAR(255),
    subject VARCHAR(1024),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Offline message queue table
CREATE TABLE offline_queue (
    recipient_id UUID NOT NULL,
    message_id UUID NOT NULL,
    PRIMARY KEY (recipient_id, message_id),
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    FOREIGN KEY (message_id) REFERENCES messages(id)
);

-- Encryption methods comparison table
CREATE TABLE encryption_benchmarks (
    id UUID PRIMARY KEY,
    method_name VARCHAR(100) NOT NULL,
    test_input_size_bytes INTEGER NOT NULL,
    encryption_time_ms BIGINT,
    decryption_time_ms BIGINT,
    ciphertext_size_bytes INTEGER,
    security_level VARCHAR(50),
    is_hardware_compatible BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_recipient ON messages(recipient_id);
CREATE INDEX idx_messages_status ON messages(delivery_status);
CREATE INDEX idx_audit_type ON audit_events(event_type);
CREATE INDEX idx_audit_actor ON audit_events(actor);
CREATE INDEX idx_offline_queue_recipient ON offline_queue(recipient_id);
CREATE INDEX idx_file_transfers_sender ON file_transfers(sender_id);
CREATE INDEX idx_group_members_group ON group_members(group_id);

