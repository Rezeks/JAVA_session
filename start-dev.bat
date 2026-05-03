@echo off
title Secure Messaging System - DEV mode

echo ============================================
echo   Secure Messaging System - DEV
echo   Local Startup (without app container)
echo ============================================
echo.

:: Check Java
java --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 21 is not installed!
    pause
    exit /b 1
)

:: Check Maven
call mvn --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed!
    pause
    exit /b 1
)

echo [1/3] Starting infrastructure (PostgreSQL + Kafka)...
docker compose up -d postgres zookeeper kafka
if errorlevel 1 (
    echo [ERROR] Failed to start infrastructure!
    echo Ensure Docker Desktop is running.
    pause
    exit /b 1
)

echo.
echo [2/3] Waiting for PostgreSQL to be ready...
:wait_pg
timeout /t 2 /nobreak >nul 2>&1
docker compose exec -T postgres pg_isready -U postgres -d securemsg >nul 2>&1
if errorlevel 1 (
    echo    ... PostgreSQL is starting up
    goto wait_pg
)
echo    PostgreSQL is ready!

echo.
echo [3/3] Starting application (mvn spring-boot:run)...
echo.
echo ============================================
echo   PostgreSQL:   localhost:5432
echo   Kafka:        localhost:9092
echo   Application:  http://localhost:8080
echo   Swagger UI:   http://localhost:8080/swagger-ui.html
echo ============================================
echo.

call mvn spring-boot:run
pause
