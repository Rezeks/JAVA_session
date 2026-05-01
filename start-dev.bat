@echo off
chcp 65001 >nul 2>&1
title Secure Messaging System - DEV режим

echo ============================================
echo   Secure Messaging System - DEV
echo   Локальный запуск (без контейнера приложения)
echo ============================================
echo.

:: Check Java
java --version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Java 21 не установлена!
    pause
    exit /b 1
)

:: Check Maven
call mvn --version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Maven не установлен!
    pause
    exit /b 1
)

echo [1/3] Запускаю инфраструктуру (PostgreSQL + Kafka)...
docker compose up -d postgres zookeeper kafka
if errorlevel 1 (
    echo [ОШИБКА] Не удалось запустить инфраструктуру!
    echo Убедитесь что Docker Desktop запущен.
    pause
    exit /b 1
)

echo.
echo [2/3] Ожидаю готовность PostgreSQL...
:wait_pg
timeout /t 2 /nobreak >nul 2>&1
docker compose exec -T postgres pg_isready -U postgres -d securemsg >nul 2>&1
if errorlevel 1 (
    echo    ... PostgreSQL ещё запускается
    goto wait_pg
)
echo    PostgreSQL готов!

echo.
echo [3/3] Запускаю приложение (mvn spring-boot:run)...
echo.
echo ============================================
echo   PostgreSQL:   localhost:5432
echo   Kafka:        localhost:9092
echo   Приложение:   http://localhost:8080
echo   Swagger UI:   http://localhost:8080/swagger-ui.html
echo ============================================
echo.

call mvn spring-boot:run
pause
