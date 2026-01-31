#!/bin/bash

# Complete Environment Startup Script
# Starts Docker services and the Spring Boot application

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "🚀 Starting Complete Environment"
echo "========================================="

# Step 1: Start Docker services
echo ""
echo "[1/3] Starting Docker services (RocketMQ + MySQL)..."
./docker-start.sh

# Step 2: Rebuild application (if needed)
echo ""
echo "[2/3] Checking if rebuild is needed..."
if [ ! -f target/task-async-service-1.0.0.jar ] || [ src/main/java -nt target/task-async-service-1.0.0.jar ]; then
    echo "📦 Building application..."
    mvn clean package -DskipTests -q
else
    echo "✅ Application is already built"
fi

# Step 3: Start application
echo ""
echo "[3/3] Starting Spring Boot application..."
echo "📍 Logs will appear below. Press Ctrl+C to stop."
echo ""

# Find any running instances and stop them
PID=$(ps aux | grep 'task-async-service-1.0.0.jar' | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "⚠️  Stopping existing instance (PID: $PID)..."
    kill $PID 2>/dev/null || true
    sleep 2
fi

# Start application in foreground
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
