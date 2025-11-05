@echo off
REM ========================================
REM Auth Server - Docker Quick Start Script (Windows)
REM ========================================

echo ============================================
echo 🚀 Auth Server - Docker Deployment
echo ============================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Error: Docker is not installed
    echo Please install Docker Desktop: https://docs.docker.com/desktop/install/windows-install/
    exit /b 1
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo ❌ Error: Docker Compose is not installed
        echo Please install Docker Compose
        exit /b 1
    )
)

REM Check if .env file exists
if not exist .env (
    echo ⚠️ Warning: .env file not found
    echo Please create a .env file with your configuration
    echo.
    echo Required variables:
    echo   - REDIS_URL
    echo   - ZITADEL_INSTANCE_URL
    echo   - ZITADEL_ACCESS_TOKEN
    echo   - And all persona-specific variables
    echo.
    set /p continue=Continue without .env file? (y/N): 
    if /i not "%continue%"=="y" exit /b 1
)

echo.
echo 📦 Building Docker images...
docker-compose build
if errorlevel 1 (
    echo ❌ Error: Failed to build images
    exit /b 1
)

echo.
echo 🏃 Starting services...
docker-compose up -d
if errorlevel 1 (
    echo ❌ Error: Failed to start services
    exit /b 1
)

echo.
echo ⏳ Waiting for services to be healthy...
timeout /t 5 /nobreak >nul

echo.
echo ✅ Auth Server is starting!
echo.
echo 📍 Service URLs:
echo    - API: http://localhost:8080
echo    - Health: http://localhost:8080/actuator/health
echo.
echo 📋 Useful commands:
echo    - View logs: docker-compose logs -f auth-server
echo    - Stop services: docker-compose down
echo    - Restart: docker-compose restart auth-server
echo.
echo 🎉 Deployment complete!
echo.

pause

