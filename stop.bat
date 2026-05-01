@echo off
chcp 65001 >nul 2>&1
title Secure Messaging System - Остановка

echo ============================================
echo   Остановка всех сервисов
echo ============================================
echo.

docker compose down
if errorlevel 1 (
    echo [ОШИБКА] Не удалось остановить контейнеры
    pause
    exit /b 1
)

echo.
echo   Все контейнеры остановлены.
echo   Данные PostgreSQL сохранены в Docker volume.
echo.
echo   Для полного удаления данных:
echo   docker compose down -v
echo.
pause
