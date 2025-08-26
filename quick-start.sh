#!/bin/bash

# MSMQ Manager - Quick Start Script
# This script helps developers quickly set up and run the MSMQ Manager application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 17 or higher is required. Found: $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java is not installed"
        exit 1
    fi
    
    # Check Maven
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
        print_success "Maven $MAVEN_VERSION found"
    else
        print_error "Maven is not installed"
        exit 1
    fi
    
    # Check Docker (optional)
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
        print_success "Docker $DOCKER_VERSION found"
        DOCKER_AVAILABLE=true
    else
        print_warning "Docker not found. Docker deployment will be skipped"
        DOCKER_AVAILABLE=false
    fi
    
    # Check Docker Compose (optional)
    if command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)
        print_success "Docker Compose $COMPOSE_VERSION found"
        COMPOSE_AVAILABLE=true
    else
        print_warning "Docker Compose not found. Docker Compose deployment will be skipped"
        COMPOSE_AVAILABLE=false
    fi
}

# Function to build the application
build_application() {
    print_status "Building MSMQ Manager application..."
    
    if mvn clean install -DskipTests; then
        print_success "Application built successfully"
    else
        print_error "Build failed"
        exit 1
    fi
}

# Function to run the application locally
run_local() {
    print_status "Starting MSMQ Manager application locally..."
    
    # Check if port 8080 is available
    if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "Port 8080 is already in use. Please stop the service using that port first."
        exit 1
    fi
    
    print_status "Application will be available at:"
    echo "  - Main Application: http://localhost:8080/msmq-manager"
    echo "  - Health Check: http://localhost:8080/msmq-manager/actuator/health"
    echo "  - Metrics: http://localhost:8080/msmq-manager/actuator/prometheus"
    echo ""
    print_status "Press Ctrl+C to stop the application"
    echo ""
    
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
}

# Function to run with Docker Compose
run_docker() {
    if [ "$COMPOSE_AVAILABLE" = false ]; then
        print_error "Docker Compose is not available"
        return 1
    fi
    
    print_status "Starting MSMQ Manager with Docker Compose..."
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        return 1
    fi
    
    # Build and start services
    if docker-compose up -d --build; then
        print_success "Docker services started successfully"
        echo ""
        print_status "Services are available at:"
        echo "  - MSMQ Manager: http://localhost:8080/msmq-manager"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Grafana: http://localhost:3000 (admin/admin123)"
        echo "  - H2 Database: http://localhost:8181"
        echo ""
        print_status "To view logs: docker-compose logs -f msmq-manager"
        print_status "To stop services: docker-compose down"
    else
        print_error "Failed to start Docker services"
        return 1
    fi
}

# Function to run tests
run_tests() {
    print_status "Running tests..."
    
    if mvn test; then
        print_success "All tests passed"
    else
        print_error "Tests failed"
        exit 1
    fi
}

# Function to show help
show_help() {
    echo "MSMQ Manager - Quick Start Script"
    echo ""
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -b, --build    Build the application only"
    echo "  -t, --test     Run tests only"
    echo "  -l, --local    Run application locally"
    echo "  -d, --docker   Run application with Docker Compose"
    echo "  -a, --all      Build, test, and run locally"
    echo ""
    echo "Examples:"
    echo "  $0 --all       # Complete setup and run locally"
    echo "  $0 --docker    # Run with Docker Compose"
    echo "  $0 --build     # Build only"
    echo ""
}

# Main script logic
main() {
    case "${1:-}" in
        -h|--help)
            show_help
            exit 0
            ;;
        -b|--build)
            check_prerequisites
            build_application
            ;;
        -t|--test)
            check_prerequisites
            build_application
            run_tests
            ;;
        -l|--local)
            check_prerequisites
            build_application
            run_local
            ;;
        -d|--docker)
            check_prerequisites
            run_docker
            ;;
        -a|--all)
            check_prerequisites
            build_application
            run_tests
            run_local
            ;;
        "")
            # No arguments provided, show help
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
}

# Trap Ctrl+C and cleanup
trap 'echo ""; print_status "Stopping application..."; exit 0' INT

# Run main function with all arguments
main "$@"
