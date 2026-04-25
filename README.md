# Защищенная система обмена сообщениями (Java/Spring Boot)

Проект сделан как локальная демонстрация для сессии: максимально простой, без продовой инфраструктуры,
но с покрытием функциональных требований из ТЗ.

## Покрытие функциональных требований

### 2.1.1 Аутентификация и управление доступом

- Регистрация с генерацией пары ключей: `UserService#register`, `KeyVault#getOrCreateSigningKeyPair`.
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

## Запуск

```bash
mvn test
mvn spring-boot:run
```

## Локальный режим

1. Kafka ожидается на `localhost:9092`.
2. Файлы пишутся в локальную папку `storage/`.
3. `KeyVault` — in-memory эмуляция HSM/TPM для учебного демо.
