@echo off
REM MSMQ Manager - Quick Start Script for Windows
REM This script helps developers quickly set up and run the MSMQ Manager application

setlocal enabledelayedexpansion

REM Set colors for output (Windows 10+)
set "BLUE=[94m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "NC=[0m"

REM Function to print colored output
:print_status
echo %BLUE%[INFO]%NC% %~1
goto :eof

:print_success
echo %GREEN%[SUCCESS]%NC% %~1
goto :eof

REM Function to check prerequisites
:check_prerequisites
call :print_status "Checking prerequisites..."

REM Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Java is not installed
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%i
    set JAVA_VERSION=!JAVA_VERSION:~1,2!
    if !JAVA_VERSION! lss 17 (
        echo %RED%[ERROR]%NC% Java 17 or higher is required. Found: !JAVA_VERSION!
        exit /b 1
    )
)
call :print_success "Java !JAVA_VERSION! found"

REM Check Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Maven is not installed
    exit /b 1
)
call :print_success "Maven found"

REM Check Docker (optional)
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    call :print_success "Docker found"
    set DOCKER_AVAILABLE=true
) else (
    echo %YELLOW%[WARNING]%NC% Docker not found. Docker deployment will be skipped
    set DOCKER_AVAILABLE=false
)

REM Check Docker Compose (optional)
docker-compose --version >nul 2>&1
if %errorlevel% equ 0 (
    call :print_success "Docker Compose found"
    set COMPOSE_AVAILABLE=true
) else (
    echo %YELLOW%[WARNING]%NC% Docker Compose not found. Docker Compose deployment will be skipped
    set COMPOSE_AVAILABLE=false
)
goto :eof

REM Function to build the application
:build_application
call :print_status "Building MSMQ Manager application..."

mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Build failed
    exit /b 1
)
call :print_success "Application built successfully"
goto :eof

REM Function to run the application locally
:run_local
call :print_status "Starting MSMQ Manager application locally..."

REM Check if port 8080 is available
netstat -an | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo %YELLOW%[WARNING]%NC% Port 8080 is already in use. Please stop the service using that port first.
    exit /b 1
)

call :print_status "Application will be available at:"
echo   - Main Application: http://localhost:8080/msmq-manager
echo   - Health Check: http://localhost:8080/msmq-manager/actuator/health
echo   - Metrics: http://localhost:8080/msmq-manager/actuator/prometheus
echo.
call :print_status "Press Ctrl+C to stop the application"
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=dev
goto :eof

REM Function to run with Docker Compose
:run_docker
if "%COMPOSE_AVAILABLE%"=="false" (
    echo %RED%[ERROR]%NC% Docker Compose is not available
    exit /b 1
)

call :print_status "Starting MSMQ Manager with Docker Compose..."

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Docker is not running. Please start Docker first.
    exit /b 1
)

REM Build and start services
docker-compose up -d --build
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Failed to start Docker services
    exit /b 1
)

call :print_success "Docker services started successfully"
echo.
call :print_status "Services are available at:"
echo   - MSMQ Manager: http://localhost:8080/msmq-manager
echo   - Prometheus: http://localhost:9090
echo   - Grafana: http://localhost:3000 (admin/admin123)
echo   - H2 Database: http://localhost:8181
echo.
call :print_status "To view logs: docker-compose logs -f msmq-manager"
call :print_status "To stop services: docker-compose down"
goto :eof

REM Function to run tests
:run_tests
call :print_status "Running tests..."

mvn test
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%NC% Tests failed
    exit /b 1
)
call :print_success "All tests passed"
goto :eof

REM Function to show help
:show_help
echo MSMQ Manager - Quick Start Script
echo.
echo Usage: %~nx0 [OPTION]
echo.
echo Options:
echo   -h, --help     Show this help message
echo   -b, --build    Build the application only
echo   -t, --test     Run tests only
echo   -l, --local    Run application locally
echo   -d, --docker   Run application with Docker Compose
echo   -a, --all      Build, test, and run locally
echo.
echo Examples:
echo   %~nx0 --all       # Complete setup and run locally
echo   %~nx0 --docker    # Run with Docker Compose
echo   %~nx0 --build     # Build only
echo.
goto :eof

REM Main script logic
:main
if "%1"=="" goto :show_help
if "%1"=="-h" goto :show_help
if "%1"=="--help" goto :show_help
if "%1"=="-b" goto :build
if "%1"=="--build" goto :build
if "%1"=="-t" goto :test
if "%1"=="--test" goto :test
if "%1"=="-l" goto :local
if "%1"=="--local" goto :local
if "%1"=="-d" goto :docker
if "%1"=="--docker" goto :docker
if "%1"=="-a" goto :all
if "%1"=="--all" goto :all

echo %RED%[ERROR]%NC% Unknown option: %1
call :show_help
exit /b 1

:build
call :check_prerequisites
call :build_application
goto :end

:test
call :check_prerequisites
call :build_application
call :run_tests
goto :end

:local
call :check_prerequisites
call :build_application
call :run_local
goto :end

:docker
call :check_prerequisites
call :run_docker
goto :end

:all
call :check_prerequisites
call :build_application
call :run_tests
call :run_local
goto :end

:end
exit /b 0
