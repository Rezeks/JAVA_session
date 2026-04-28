# Защищенная система обмена сообщениями (Java/Spring Boot)

Проект сделан как локальная демонстрация для сессии: максимально простой, без prodсовой инфраструктуры,
но с полным покрытием функциональных требований защищённого мессенджера.

**🆕 НОВОЕ**: PostgreSQL интегрирована, реализовано сравнение 6+ методов шифрования с анализом слабостей.

## 🚀 БЫСТРЫЙ СТАРТ

### Вариант 1: С Docker (рекомендуется)

```bash
# Windows
start-with-postgres.bat

# Linux/Mac
bash start-with-postgres.sh
```

Это автоматически запустит Kafka + PostgreSQL + приложение.

### Вариант 2: Вручную

```bash
# Запусти контейнеры
docker-compose up -d

# В другом терминале: приложение
mvn spring-boot:run

# Открой браузер
http://localhost:8080
```

---

## 🔐 НОВОЕ: Анализ методов шифрования

**Открой в браузере:** `http://localhost:8080/ui/encryption-comparison.html`

Полный анализ 6+ методов:
- **AES-256-GCM** ✅ (рекомендуется)
- **AES-256-CBC** ✅ (хороший)
- **AES-128-CBC** ⚠️ (слабеет)
- **DES** ❌ (взломан)
- **ChaCha20** ✅ (для мобил)
- **RSA-4096** (только для ключей)

Для каждого: таблица слабостей, преимущества/недостатки, тестирование на реальных текстах.

📖 Подробнее: **[POSTGRESQL_AND_ENCRYPTION_GUIDE.md](./POSTGRESQL_AND_ENCRYPTION_GUIDE.md)**

---

## 📊 Быстрый ответ на вопрос "Соответствует ли система требованиям?"

**✅ ДА.** [Полный анализ в COMPLIANCE_CHECKLIST.md](./COMPLIANCE_CHECKLIST.md)

- ✅ Защита переписки: **AES-256-GCM + RSA-2048 SignAuth**
- ✅ Forward Secrecy: **одноразовые ключи на сообщение**
- ⚠️  Офлайн-режим: **есть очередь, но in-memory** (todo: DB)
- ✅ Защита от взлома: **PBKDF2 пароли + двухфакторная аутентификация + шифрование**
- ✅ Аппаратная защита: **готово для HSM/TPM** (текущее: эмуляция)

### 📚 Важные документы для анализа

| Документ | Назначение |
|----------|-----------|
| [**COMPLIANCE_CHECKLIST.md**](./COMPLIANCE_CHECKLIST.md) | ✅ Ответ на вопрос "Соответствует ли всем критериям?" |
| [**SECURITY_ANALYSIS.md**](./SECURITY_ANALYSIS.md) | 🔐 Детальный анализ по каждому требованию (2.1.1 - 2.1.4) |
| [**ARCHITECTURE.md**](./ARCHITECTURE.md) | 🏗️ Диаграммы компонентов, криптография, flow-диаграммы |
| [**DEMO_SCENARIO.md**](./DEMO_SCENARIO.md) | 🎬 Сценарий демонстрации на сессии (7-10 минут пошагово) |

## Покрытие функциональных требований

### 2.1.1 Аутентификация и управление доступом

- Регистрация с генерацией пары ключей: `UserService#register`, `KeyVault#getOrCreateSigningKeyPair`.
- **Хеширование паролей**: PBKDF2-HMAC-SHA256 (100,000 итераций + 32-byte salt) ✅
  - Вместо простого SHA-256 по лучшей практике NIST SP 800-132
- Двухфакторная аутентификация (пароль + аппаратный токен): `UserService#authenticate(login,password,hardwareToken)`.
- Ролевая модель доступа: `Role.ADMIN`, `Role.OPERATOR`, `Role.USER` + назначение через `UserService#assignRole`.
- Автоблокировка при неуспешных входах: `UserService` (порог 5 попыток).

### 2.1.2 Обмен сообщениями

- Отправка/прием текстовых сообщений: `MessagingService#send`, `syncHistory`, `pullOfflineMessages`.
- Групповые чаты: `createGroup`, `sendGroupMessage`.
- Офлайн-доставка через очередь получателя: `offlineQueueByRecipient`.
- Статусы доставки: `QUEUED`, `SENT`, `DELIVERED`, `READ`, `ERROR`, `DELETED`.
- Kafka интеграция (локально): события `message.sent`, `message.delivered`, `message.error`, `group.message.sent`.

### 2.1.3 Передача файлов

- Передача файлов до 2 ГБ: `FileTransferService#initiateUpload`.
- Потоковое шифрование: `CryptoService#encryptStream`.
- Возобновление передачи с offset: `uploadChunk(transferId, offset, inputStream)`.
- Проверка хеша файла у получателя: `verifyChecksum`.

### 2.1.4 Криптографические операции

- Генерация/импорт/экспорт ключевых пар: `KeyVault`, `InMemoryKeyVault`.
- Подпись и проверка подписи каждого сообщения: `CryptoService#sign/verify`.
- Авто-ротация сессионного ключа не реже 1 раза в час: `MessagingService#rotateKeyIfNeeded`.
- Forward secrecy (упрощенно для демо): одноразовый ключ на сообщение + обертка RSA-OAEP (`wrappedMessageKey`).
- Post-compromise security (упрощенно для демо): `recoverAfterCompromise` в `UserService` и `MessagingService`.

## Демо API

- Пользователи: `POST /api/users/register`, `POST /api/users/{login}/confirm`, `POST /api/users/auth`,
  `POST /api/users/{login}/role`, `POST /api/users/{login}/password`, `POST /api/users/{login}/block`,
  `POST /api/users/{login}/token/rotate`, `POST /api/users/{login}/recover`.
- Сообщения: `POST /api/messages/send`, `POST /api/messages/{messageId}/deliver`,
  `POST /api/messages/{messageId}/read?readerId=...`, `POST /api/messages/{messageId}/error`,
  `GET /api/messages/history/{userId}`, `GET /api/messages/offline/{userId}`,
  `POST /api/messages/group`, `POST /api/messages/group/{groupId}/send`.
- Файлы: `POST /api/files/init`, `POST /api/files/{transferId}/chunk?offset=...`,
  `POST /api/files/{transferId}/finalize`, `GET /api/files/{transferId}/verify?checksum=...`,
  `POST /api/files/{transferId}/delivered`.
- Ключи и аудит: `GET /api/keys/{ownerId}/public`, `GET /api/keys/{ownerId}/private`,
  `POST /api/keys/{ownerId}/import`, `GET /api/audit`.

## Visual demo UI

- В проект добавлен многостраничный локальный UI в `src/main/resources/static/ui/*`.
- После запуска открой: `http://localhost:8080` (редирект на `http://localhost:8080/ui/index.html`).
- UI работает поверх существующих endpoint-ов `DemoController` (`/api/*`) и подходит для показа на сессии.

### Страницы UI

- `http://localhost:8080/ui/users.html` - регистрация, confirm, 2FA, роли, block/recover.
- `http://localhost:8080/ui/messages.html` - send/deliver/read/error, history, offline pull.
- `http://localhost:8080/ui/groups.html` - create group и send group message.
- `http://localhost:8080/ui/files.html` - init/chunk/finalize/verify/delivered для файлов.
- `http://localhost:8080/ui/audit.html` - export/import ключей и просмотр аудита.

### Быстрый сценарий показа

1. Открой `users.html`: зарегистрируй 2 пользователей, confirm, auth 2FA.
2. Открой `messages.html`: отправь сообщение, затем deliver/read и покажи history.
3. Открой `groups.html`: создай группу и отправь group message.
4. Открой `files.html`: выполни pipeline загрузки файла.
5. Открой `audit.html`: покажи ключи и журнал событий.

## Быстрый старт для демо

```bash
mvn test
mvn spring-boot:run
```

Открой в браузере: `http://localhost:8080`

### Сценарий демонстрации (7-10 минут)

Смотри [DEMO_SCENARIO.md](./DEMO_SCENARIO.md) — полный пошаговый сценарий с объяснениями.

**Кратко:**
1. **Users** (`http://localhost:8080/ui/users.html`) - регистрация, 2FA, роли
2. **Messages** (`http://localhost:8080/ui/messages.html`) - отправка, доставка, офлайн-пулл
3. **Groups** (`http://localhost:8080/ui/groups.html`) - групповые чаты
4. **Files** (`http://localhost:8080/ui/files.html`) - загрузка файлов с шифрованием
5. **Audit** (`http://localhost:8080/ui/audit.html`) - журнал операций

## Локальный режим

1. Kafka ожидается на `localhost:9092`.
2. Файлы пишутся в локальную папку `storage/`.
3. `KeyVault` — in-memory эмуляция HSM/TPM для учебного демо.
4. Все данные хранятся в памяти (in-memory ConcurrentHashMap) — теряются при перезагрузке.

**Для production требуется:**
- PostgreSQL (персистентность) ⚠️
- mTLS сертификаты ⚠️
- Реальный HSM/TPM адаптер ⚠️
- Rate limiting ⚠️

Подробнее в [SECURITY_ANALYSIS.md](./SECURITY_ANALYSIS.md#-что-ещё-нужно-для-production)

---

## 📖 Полная документация

| Документ | Содержание |
|----------|-----------|
| **COMPLIANCE_CHECKLIST.md** | Ответ на итоговый вопрос: "Соответствует ли система требованиям?" с таблицами и вердиктом. |
| **SECURITY_ANALYSIS.md** | Детальный анализ соответствия функциональным требованиям (2.1.1 - 2.1.4). Список того что сделано (✅), что частично (⚠️) и что не сделано (❌). |
| **ARCHITECTURE.md** | Диаграммы компонентов, flow данных при отправке сообщения, криптографический flow, state machine статусов, защита от атак. |
| **DEMO_SCENARIO.md** | Пошаговый сценарий демонстрации на сессии: что открывать, что вводить, что ожидать, что говорить аудитории. |

---

## 🔐 Криптография в этом проекте

```
Шифрование: AES-256-GCM (128-bit AEAD tag)
  └─ Каждое сообщение шифруется уникальным одноразовым ключом
  └─ IV 12 bytes, генерируется случайно

Подпись: SHA-256 with RSA-2048
  └─ Каждое сообщение подписывается приватным ключом отправителя
  └─ Получатель проверяет с публичным ключом отправителя

Обертка ключей: RSA-OAEP (SHA-256)
  └─ OneTimeKey оборачивается в публичный ключ получателя
  └─ Только получатель может раскрыть своим приватным ключом
  └─ Обеспечивает Forward Secrecy

Пароли: PBKDF2-HMAC-SHA256
  └─ 100,000 итераций
  └─ 32-byte salt
  └─ Защита от перебора и rainbow tables

Ключи: RSA-2048 (в production — HSM/TPM)
  └─ Собственная пара ключей на каждого пользователя
  └─ Сейчас in-memory, в production в защищённом модуле
```

Смотри [ARCHITECTURE.md](./ARCHITECTURE.md#параметры-криптографии) для полной таблицы параметров.
