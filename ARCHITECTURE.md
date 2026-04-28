# 🏗️ Архитектура защищённой системы обмена сообщениями

## Диаграмма компонентов

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            WEB UI (Browser)                              │
│                   http://localhost:8080/ui/index.html                    │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ REST/JSON
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                             │
│                          (Port 8080)                                     │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    DemoController                                 │  │
│  │  /api/users/*  /api/messages/*  /api/files/*  /api/audit/*      │  │
│  └──────┬───────────────────────────┬────────────────┬──────────────┘  │
│         │                           │                │                  │
│         ▼                           ▼                ▼                  │
│  ┌──────────────┐  ┌─────────────────────────┐  ┌─────────────────┐   │
│  │ UserService  │  │ MessagingService        │  │FileTransfer     │   │
│  │              │  │ └─ GroupChat support    │  │Service          │   │
│  │- register    │  │ └─ Offline queue        │  │                 │   │
│  │- auth 2FA    │  │ └─ Key rotation         │  │- init upload    │   │
│  │- assignRole  │  │ └─ Recovery             │  │- chunk upload   │   │
│  │- block/      │  │                         │  │- finalize       │   │
│  │  recover     │  │                         │  │- verify         │   │
│  └──────┬───────┘  └────────────┬────────────┘  └────────┬────────┘   │
│         │                       │                        │              │
│         └───────────┬───────────┴───────────┬────────────┘              │
│                     ▼                       │                           │
│          ┌──────────────────────┐           │                           │
│          │  AuditService        │           │                           │
│          │ (all events logged)  │           │                           │
│          └──────────────────────┘           │                           │
│                                              ▼                           │
│          ┌────────────────────────────────────────────┐                 │
│          │        CryptoService                        │                 │
│          │  ┌─────────────────────────────────────┐  │                 │
│          │  │ AES-GCM encryption/decryption       │  │                 │
│          │  │ RSA-OAEP key wrapping               │  │                 │
│          │  │ SHA256withRSA signing/verification  │  │                 │
│          │  │ Stream encryption (for files)       │  │                 │
│          │  └─────────────────────────────────────┘  │                 │
│          └────────────┬───────────────────────────────┘                 │
│                       │                                                 │
│                       ▼                                                 │
│          ┌────────────────────────────────────────────┐                 │
│          │           KeyVault Interface               │                 │
│          │  ┌─────────────────────────────────────┐  │                 │
│          │  │ getOrCreateSigningKeyPair()         │  │  Pluggable:    │
│          │  │ getOrCreateEncryptionKey()          │  │  - HSM/TPM     │
│          │  │ rotateSigningKeyPair()              │  │  - AWS KMS     │
│          │  │ rotateEncryptionKey()               │  │  - Azure KV    │
│          │  │ importKey() / exportKey()           │  │                 │
│          │  └─────────────────────────────────────┘  │                 │
│          └────────────┬───────────────────────────────┘                 │
│                       │                                                 │
│                       ▼                                                 │
│       ┌──────────────────────────────────┐                             │
│       │  InMemoryKeyVault (Demo)         │                             │
│       │  ⚠️ ONLY for development/demo    │                             │
│       │  Keys stored in JVM memory       │                             │
│       └──────────────────────────────────┘                             │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │         In-Memory Storage (Data)                              │   │
│  │  - users: ConcurrentHashMap<String, User>                    │   │
│  │  - messages: ConcurrentHashMap<UUID, Message>                │   │
│  │  - groups: ConcurrentHashMap<UUID, GroupChat>                │   │
│  │  - offlineQueue: ConcurrentHashMap<UUID, List<UUID>>        │   │
│  │  - audit events: CopyOnWriteArrayList<AuditEvent>           │   │
│  │  ⚠️ ALL LOST ON RESTART                                      │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │         File Storage (Local Disk)                             │   │
│  │  - Path: ./storage/*                                          │   │
│  │  - Encrypted files + metadata                                │   │
│  │  - Accessible via FileTransferService                        │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │         Kafka Integration (Event Stream)                      │   │
│  │  - Topics: message.sent, message.delivered, message.error,   │   │
│  │            group.message.sent                                │   │
│  │  - KafkaAuditConsumer (reads and logs events)                │   │
│  │  - Bootstrap: localhost:9092                                 │   │
│  └───────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                 │
                                 │ (Events published)
                                 ▼
┌──────────────────────────────────────────────────────────────┐
│              Kafka Broker (localhost:9092)                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Topics for event sourcing (for future improvements)   │  │
│  │ - message.sent                                        │  │
│  │ - message.delivered                                   │  │
│  │ - message.error                                       │  │
│  │ - group.message.sent                                  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## Поток данных: отправка сообщения

```
User A                   Server                      User B
  │                         │                           │
  ├─ Register ────────────► │ [KeyVault]                │
  │                         │ └─ Generate RSA KeyPair   │
  │                         │                           │
  ├─ Auth 2FA ────────────► │ [PBKDF2] Verify Password  │
  │                         │                           │
  ├─ Send Message ────────► │ [User A's Private Key]    │
  │                         │ · OneTimeKey (256-bit)    │
  │                         │ · AES-GCM Encrypt         │
  │                         │ · RSA-OAEP wrap key       │
  │                         │ · SHA256withRSA sign      │
  │                         │                           │
  │                         │ [Offline Queue]           │
  │                         │ └─ Queue for User B       │
  │                         │                           │
  │                         │ [Kafka] message.sent      │
  │                         │ [Audit] log operation    │
  │                         │                           │
  │                         │ ◄───────────┐             │
  │                         │            ┌─┴── Poll Offline
  │                         │            │               │
  │                         │ [Decrypt] ─┘               │
  │                         │ · RSA-OAEP unwrap key     │
  │                         │ · Verify signature        │
  │                         │ · AES-GCM decrypt         │
  │                         │                           │
  │                         │ [User B reads message] ──►│
  │                         │                           │
```

---

## Криптографический flow

```
┌──────────────────────────────────────────────────────┐
│         MESSAGE ENCRYPTION (Send Side)               │
├──────────────────────────────────────────────────────┤
│                                                      │
│ PlainText Message                                   │
│         │                                            │
│         ▼                                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Generate OneTimeKey (256-bit)                   │ │
│ │ keyMaterial = SecureRandom.nextBytes(32)        │ │
│ └────────────┬────────────────────────────────────┘ │
│              │                                       │
│              ▼                                       │
│ ┌─────────────────────────────────────────────────┐ │
│ │ AES-256-GCM Encrypt                             │ │
│ │ IV = random 12 bytes                            │ │
│ │ ciphertext = AES.encrypt(plaintext, IV, key)   │ │
│ │ (includes authentication tag 128-bit)           │ │
│ └────────────┬────────────────────────────────────┘ │
│              │                                       │
│              │ Ciphertext                           │
│              │                                       │
│              ├─────────────────────────┐             │
│              │                         │             │
│              ▼                         ▼             │
│ ┌──────────────────────┐   ┌────────────────────┐   │
│ │ RSA-OAEP Wrap Key    │   │ SHA256withRSA Sign │   │
│ │ WrapKey =            │   │ signature =        │   │
│ │ RSA.wrap(OneTimeKey) │   │ RSA.sign(cipher)   │   │
│ │ using Recipient's    │   │ using Sender's     │   │
│ │ public key           │   │ private key        │   │
│ └──────────────────────┘   └────────────────────┘   │
│              │                         │             │
│              └─────────────┬───────────┘             │
│                            │                         │
│                            ▼                         │
│                    Store Message:                    │
│                    {                                 │
│                      id: UUID,                       │
│                      sender: UUID,                   │
│                      recipient: UUID,                │
│                      encryptedPayload: cipher,       │
│                      wrappedMessageKey: wrapped,     │
│                      signature: sig,                 │
│                      ratchetStep: int,               │
│                      status: QUEUED                  │
│                    }                                 │
│                                                      │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│       MESSAGE DECRYPTION (Receive Side)              │
├──────────────────────────────────────────────────────┤
│                                                      │
│ Retrieve Message from Offline Queue                 │
│         │                                            │
│         ▼                                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Verify Signature                                │ │
│ │ RSA.verify(signature, ciphertext,               │ │
│ │           sender's public key)                  │ │
│ │ ❌ If invalid → reject message                  │ │
│ └────────────┬────────────────────────────────────┘ │
│              │                                       │
│              ▼                                       │
│ ┌─────────────────────────────────────────────────┐ │
│ │ RSA-OAEP Unwrap OneTimeKey                      │ │
│ │ OneTimeKey = RSA.unwrap(wrappedKey,             │ │
│ │            my private key)                      │ │
│ └────────────┬────────────────────────────────────┘ │
│              │                                       │
│              ▼                                       │
│ ┌─────────────────────────────────────────────────┐ │
│ │ AES-256-GCM Decrypt                             │ │
│ │ plaintext = AES.decrypt(ciphertext, IV, key)   │ │
│ │ (verifies AEAD tag automatically)               │ │
│ │ ❌ If tampered → throws exception                │ │
│ └────────────┬────────────────────────────────────┘ │
│              │                                       │
│              ▼                                       │
│         PlainText Message                           │
│         (Safe to display)                           │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## Параметры криптографии

| Параметр | Значение | Стандарт | Комментарий |
|---------|----------|----------|-------------|
| **Шифрование** |
| Алгоритм | AES-256-GCM | NIST SP 800-38D | Authenticated encryption |
| Размер ключа | 256 bit | - | Strong security |
| IV размер | 12 bytes | - | Standard for GCM |
| GAG tag | 128 bit | - | Full authentication |
| **Подпись** |
| Алгоритм | RSA-SHA256 | PKCS #1 | Signing & verification |
| Размер ключа | 2048 bit | - | Adequate for demo (4096 ✓) |
| Хешфункция | SHA-256 | NIST FIPS 180-4 | US standard |
| **Обертка ключей** |
| Алгоритм | RSA-OAEP | RFC 3447 | Forward secrecy |
| Хешфункция | SHA-256 | - | Safe & fast |
| MGF1 | SHA-256 | - | Mask generation |
| **Хеширование паролей** |
| Алгоритм | PBKDF2 | NIST SP 800-132 | Key derivation |
| Функция мешивания | HMAC-SHA256 | - | Cryptographically secure |
| Итерации | 100,000 | - | Slows down brute force |
| Размер соли | 32 bytes | - | Prevents rainbow tables |

---

## Состояния сообщения (State Machine)

```
┌─────────┐
│ QUEUED  │  ◄──── Message created, waiting for delivery
└────┬────┘
     │ (Consumer pulls from offline queue)
     ▼
┌─────────┐
│  SENT   │  ◄──── Message delivered to server
└────┬────┘
     │ (Recipient confirms receipt)
     ▼
┌──────────┐
│DELIVERED │  ◄──── Message received by recipient
└────┬─────┘
     │ (Recipient opens/reads message)
     ▼
┌─────────┐
│  READ   │  ◄──── Message was read
└────┬────┘
     │
     │ (Optional: User deletes message)
     │
     ├──────────────┐
     │              │
     ▼              ▼
┌─────────┐   ┌─────────┐
│DELETED  │   │ ERROR   │  ◄──── Decryption/verification failed
└─────────┘   └─────────┘
```

---

## Защита от атак

| Атака | Механизм защиты | Статус |
|------|------------------|--------|
| **MITM (Man-in-the-Middle)** | RSA подпись + мТLS required | ✅ (mTLS todo) |
| **Перехват трафика** | AES-GCM шифрование | ✅ |
| **Tampering с сообщением** | AEAD (GCM tag) + RSA signature | ✅ |
| **Брутфорс паролей** | PBKDF2 (100k iter) + rate limit | ✅ (rate limit todo) |
| **Rainbow tables** | 32-byte salt + PBKDF2 | ✅ |
| **Компрометация ключей** | Forward secrecy (одноразовые ключи) | ✅ |
| **Разрыв соединения (офлайн)** | Offline queue (in-memory) | ⚠️ (need DB) |
| **Потеря сообщений** | Kafka event stream | ⚠️ (not persistent) |
| **Доступ к приватным ключам** | KeyVault интерфейс (HSM ready) | ✅ (need HSM) |

---

