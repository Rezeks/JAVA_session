@echo off
REM 🚀 БЫСТРЫЙ СТАРТ ПРОЕКТА (Windows)

setlocal enabledelayedexpansion

echo ==================================
echo Защищённая система обмена сообщениями
echo ==================================
echo.

REM Проверка Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven не найден в PATH
    echo    Установи Maven и добавь в PATH
    pause
    exit /b 1
)

echo ✅ Maven найден

REM Вывод версии Java
java -version
echo.

REM Компиляция
echo 📦 Компилирую проект...
call mvn -q -DskipTests clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Ошибка компиляции
    pause
    exit /b 1
)
echo ✅ Компиляция успешна
echo.

REM Информация перед запуском
echo ==================================
echo ℹ️  ИНФОРМАЦИЯ ДО ЗАПУСКА
echo ==================================
echo.
echo 1. Убедись, что Kafka запущена:
echo    Kafka должна быть доступна на localhost:9092
echo.
echo 2. После запуска откройте браузер:
echo    🌐 http://localhost:8080
echo.
echo 3. Документация:
echo    📖 START_HERE.md      - начните отсюда
echo    ⚡ EXECUTIVE_SUMMARY.md - быстрый ответ (2-5 мин)
echo    🎬 DEMO_SCENARIO.md   - пошаговая демонстрация
echo.
echo 4. API endpoints:
echo    👥 /api/users/*
echo    💬 /api/messages/*
echo    📁 /api/files/*
echo    📋 /api/audit
echo.

REM Запуск приложения
echo 🚀 Запускаю приложение...
echo    (Ctrl+C для остановки)
echo.
echo ==================================
echo.

call mvn spring-boot:run

pause

