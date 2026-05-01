@echo off
chcp 65001 >nul 2>&1
title Secure Messaging System - Тесты

echo ============================================
echo   Запуск тестов
echo ============================================
echo.

call mvn clean test
echo.

if errorlevel 1 (
    echo   [FAIL] Тесты не прошли!
) else (
    echo   [OK] Все тесты пройдены!
)
echo.
pause
