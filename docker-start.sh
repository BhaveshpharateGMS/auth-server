#!/bin/bash

# ========================================
# Auth Server - Docker Quick Start Script
# ========================================

set -e

echo "🚀 Auth Server - Docker Deployment"
echo "===================================="

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker is not installed"
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo "❌ Error: Docker Compose is not installed"
    echo "Please install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  Warning: .env file not found"
    echo "Please create a .env file with your configuration"
    echo ""
    echo "Required variables:"
    echo "  - REDIS_URL"
    echo "  - ZITADEL_INSTANCE_URL"
    echo "  - ZITADEL_ACCESS_TOKEN"
    echo "  - And all persona-specific variables (VENDOR, CONSUMER, AFFILIATE, GMS)"
    echo ""
    read -p "Continue without .env file? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "📦 Building Docker images..."
docker-compose build

echo ""
echo "🏃 Starting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 5

# Check if auth-server is running
if docker-compose ps | grep -q "auth-server.*Up"; then
    echo "✅ Auth Server is running!"
    echo ""
    echo "📍 Service URLs:"
    echo "   - API: http://localhost:8080"
    echo "   - Health: http://localhost:8080/actuator/health"
    echo ""
    echo "📋 Useful commands:"
    echo "   - View logs: docker-compose logs -f auth-server"
    echo "   - Stop services: docker-compose down"
    echo "   - Restart: docker-compose restart auth-server"
    echo ""
else
    echo "❌ Error: Auth Server failed to start"
    echo ""
    echo "Check logs with: docker-compose logs auth-server"
    exit 1
fi

echo "🎉 Deployment complete!"

