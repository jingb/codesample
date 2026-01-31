#!/bin/bash

# Docker Environment Startup Script
# This script starts all required services using Docker Compose

set -e

echo "========================================="
echo "Starting Docker Environment..."
echo "========================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"

# Start services
echo ""
echo "🚀 Starting RocketMQ and MySQL services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 15

# Check service status
echo ""
echo "========================================="
echo "Service Status:"
echo "========================================="
docker-compose ps

echo ""
echo "🔍 Verifying services..."
if nc -zv localhost 9876 2>&1 | grep -q "succeeded"; then
    echo "✅ NameServer is reachable"
else
    echo "⚠️  NameServer may still be starting"
fi

if nc -zv localhost 10911 2>&1 | grep -q "succeeded"; then
    echo "✅ Broker is reachable"
else
    echo "⚠️  Broker may still be starting"
fi

echo ""
echo "========================================="
echo "✅ Docker environment started successfully!"
echo "========================================="
echo ""
echo "📋 Service URLs:"
echo "  - RocketMQ NameServer: localhost:9876"
echo "  - RocketMQ Broker:     localhost:10911"
echo "  - RocketMQ Console:    http://localhost:8081"
echo "  - MySQL Database:      localhost:3306"
echo ""
echo "📝 MySQL Credentials:"
echo "  - Database: task_db"
echo "  - Username: task_user"
echo "  - Password: task_pass"
echo "  - Root Password: root123"
echo ""
echo "🔧 Next Steps:"
echo "  1. Run the application: mvn spring-boot:run"
echo "  2. Or build and run:    mvn clean package && java -jar target/task-async-service-1.0.0.jar"
echo ""
echo "🛑 To stop all services: docker-compose down"
echo "========================================="
