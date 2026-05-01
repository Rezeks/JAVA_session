@echo off
chcp 65001 >nul 2>&1
title Secure Messaging System - Запуск

echo ============================================
echo   Secure Messaging System
echo   Запуск всех сервисов (Docker)
echo ============================================
echo.

:: Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Docker не установлен или не запущен!
    echo Скачайте Docker Desktop: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

:: Check Docker Compose
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Docker Compose не найден!
    pause
    exit /b 1
)

echo [1/3] Собираю Docker-образ приложения...
docker compose build app
if errorlevel 1 (
    echo [ОШИБКА] Сборка образа завершилась с ошибкой!
    pause
    exit /b 1
)

echo.
echo [2/3] Запускаю PostgreSQL, Kafka, Application...
docker compose up -d
if errorlevel 1 (
    echo [ОШИБКА] Запуск контейнеров завершился с ошибкой!
    pause
    exit /b 1
)

echo.
echo [3/3] Ожидаю готовность приложения...
:wait_loop
timeout /t 3 /nobreak >nul 2>&1
curl -s http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo    ... приложение ещё запускается
    goto wait_loop
)

echo.
echo ============================================
echo   ВСЕ СЕРВИСЫ ЗАПУЩЕНЫ!
echo ============================================
echo.
echo   Приложение:        http://localhost:8080
echo   Swagger UI:        http://localhost:8080/swagger-ui.html
echo   Health Check:      http://localhost:8080/actuator/health
echo   API Docs (JSON):   http://localhost:8080/api-docs
echo.
echo   PostgreSQL:        localhost:5432
echo   Kafka:             localhost:9092
echo.
echo   Для остановки:     stop.bat
echo ============================================
echo.
pause
