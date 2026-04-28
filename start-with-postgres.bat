@echo off
REM 🚀 БЫСТРЫЙ СТАРТ: PostgreSQL + Методы шифрования (Windows)

setlocal enabledelayedexpansion

cls
echo ==================================
echo 🚀 Защищённая система обмена сообщениями
echo    с PostgreSQL + Сравнение методов шифрования
echo ==================================
echo.

REM ШАГ 1: Проверка Docker
echo 1️⃣  Проверяю наличие Docker...
docker --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker не найден
    echo    Установи Docker Desktop с https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)
echo ✅ Docker найден
echo.

REM ШАГ 2: Проверка Maven и Java
echo 2️⃣  Проверяю наличие Maven и Java...
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven не найден в PATH
    echo    Установи Maven и добавь в PATH
    pause
    exit /b 1
)
echo ✅ Maven найден
java -version
echo.

REM ШАГ 3: Запуск Docker Compose
echo 3️⃣  Запускаю Kafka + PostgreSQL...
docker-compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Ошибка при запуске Docker Compose
    echo    Проверь что docker-compose.yml существует
    pause
    exit /b 1
)
echo.
echo ⏳ Даю 15 секунд чтобы сервисы загрузились...
timeout /t 15 /nobreak
echo ✅ Сервисы запущены
echo.

REM ШАГ 4: Компиляция
echo 4️⃣  Компилирую проект...
call mvn -q -DskipTests clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Ошибка компиляции
    pause
    exit /b 1
)
echo ✅ Проект скомпилирован
echo.

REM ШАГ 5: Информация перед запуском
echo 5️⃣  Подготовка к запуску...
echo.
echo ════════════════════════════════════════════════
echo 🚀 ГОТОВО К ЗАПУСКУ
echo ════════════════════════════════════════════════
echo.
echo 🌐 После запуска откройте браузер:
echo.
echo    💻 http://localhost:8080
echo.
echo    🔐 Сравнение методов шифрования:
echo       http://localhost:8080/ui/encryption-comparison.html
echo.
echo 📊 Сервисы:
echo    - Kafka:      localhost:9092
echo    - PostgreSQL: localhost:5432
echo.
echo ❌ Для остановки: нажми Ctrl+C
echo ════════════════════════════════════════════════
echo.

REM ШАГ 6: Запуск приложения
echo Запускаю приложение...
call mvn spring-boot:run

pause

