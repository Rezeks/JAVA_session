# ⚡ QUICK START COMMANDS

## Windows (PowerShell)

```powershell
# Перейти в проект
cd "C:\Users\Rezeks\IdeaProjects\JAVA_session"

# Компилировать
mvn -q -DskipTests clean compile

# Запустить
mvn spring-boot:run

# Или просто дважды щёлкнуть run.bat
```

## Linux / Mac

```bash
cd ~/IdeaProjects/JAVA_session

mvn -q -DskipTests clean compile

mvn spring-boot:run

# Или
bash run.sh
```

## После запуска

Откройте браузер:
```
http://localhost:8080
```

---

## ДОКУМЕНТАЦИЯ (прочитай в этом порядке)

1. **START_HERE.md** (2 мин) — вход в проект
2. **DEMO_SCENARIO.md** (15 мин подготовки) — пошаговая демонстрация
3. **ARCHITECTURE.md** (10 мин) — диаграммы и криптография
4. **PRE_DEMO_CHECKLIST.md** (5 мин) — перед каждым показом

---

## ОТВЕТ НА ГЛАВНЫЙ ВОПРОС

✅ **Да, программа соответствует требованиям защищённого мессенджера.**

Подробнее в [EXECUTIVE_SUMMARY.md](./EXECUTIVE_SUMMARY.md) (2-5 мин)

---

## КРИПТОГРАФИЯ

- AES-256-GCM (шифрование)
- RSA-2048 (подпись)
- PBKDF2-HMAC-SHA256 (пароли)
- RSA-OAEP (обертка ключей)

---

## ТЕСТОВЫЕ ЛОГИНЫ

```
alice
Пароль: secure_password_123

bob
Пароль: bob_secret_789
```

---

## ЕСЛИ ЧТО-ТО НЕ РАБОТАЕТ

1. Kafka должна быть на localhost:9092
2. Java 21+ должна быть установлена
3. Maven должна быть в PATH
4. Порт 8080 должен быть свободен

Проверка:
```powershell
java -version
mvn -version
telnet localhost 9092
```

---

*Проект готов к демонстрации. Начни с START_HERE.md*

