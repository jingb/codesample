#!/bin/bash

# Complete Environment Startup Script
# 构建应用并启动所有 Docker 服务（包括应用容器）

set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "========================================="
echo "🚀 Starting Complete Environment"
echo "========================================="

# Step 1: Build application
echo ""
echo "[1/3] Building application..."
if [ ! -f target/task-async-service-1.0.0.jar ] || [ src/main/java -nt target/task-async-service-1.0.0.jar ]; then
    echo "📦 Maven build in progress..."
    mvn clean package -DskipTests
    echo "✅ Build completed"
else
    echo "✅ Application JAR is already up-to-date"
fi

# Step 2: Start all Docker services
echo ""
echo "[2/3] Starting Docker services..."
docker-compose down 2>/dev/null || true
docker-compose up -d --build

# Step 3: Wait for services to be healthy
echo ""
echo "[3/3] Waiting for services to be ready..."
echo "⏳ This may take 30-60 seconds..."

# Wait and check service status
MAX_WAIT=70
WAIT_TIME=0
while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))

    # Get service status
    STATUS=$(docker-compose ps)

    # Check if all services are running and healthy
    NAMESRV_STATUS=$(echo "$STATUS" | grep namesrv | grep -c healthy || true)
    BROKER_STATUS=$(echo "$STATUS" | grep broker | grep -c healthy || true)
    APP_STATUS=$(echo "$STATUS" | grep "task-app" | grep -c healthy || true)
    MYSQL_STATUS=$(echo "$STATUS" | grep mysql | grep -c healthy || true)

    # Ensure variables are numbers (not empty)
    NAMESRV_STATUS=${NAMESRV_STATUS:-0}
    BROKER_STATUS=${BROKER_STATUS:-0}
    APP_STATUS=${APP_STATUS:-0}
    MYSQL_STATUS=${MYSQL_STATUS:-0}

    # Count total healthy services
    HEALTHY_COUNT=$((NAMESRV_STATUS + BROKER_STATUS + APP_STATUS + MYSQL_STATUS))

    echo "⏳ Progress: $HEALTHY_COUNT/4 services healthy (${WAIT_TIME}s)"

    # All services are healthy
    if [ $HEALTHY_COUNT -eq 4 ]; then
        echo "✅ All services are healthy!"
        break
    fi
done

# Final verification
if [ $HEALTHY_COUNT -lt 4 ]; then
    echo ""
    echo "⚠️  Some services may not be fully ready. Current status:"
    docker-compose ps

    # If app is not healthy, try restarting it once
    if [ $APP_STATUS -eq 0 ]; then
        echo ""
        echo "⚠️  Application not healthy, restarting..."
        docker-compose restart app
        echo "⏳ Waiting 20 seconds for app to restart..."
        sleep 20

        # Check again
        FINAL_STATUS=$(docker-compose ps)
        APP_FINAL=$(echo "$FINAL_STATUS" | grep "task-app" | grep -c healthy || true)
        APP_FINAL=${APP_FINAL:-0}

        if [ $APP_FINAL -eq 1 ]; then
            echo "✅ Application recovered after restart!"
        else
            echo "⚠️  Application still not healthy. Check logs with: docker-compose logs app"
        fi
    fi
fi

echo ""
echo "========================================="
echo "✅ Services started!"
echo "========================================="

echo ""
echo "📋 Service URLs:"
echo "  - Application API:     http://localhost:8080"
echo "  - RocketMQ Console:    http://localhost:8081"
echo "  - RocketMQ NameServer: localhost:9876"
echo "  - MySQL Database:      localhost:3306"
echo ""
echo "📝 MySQL Credentials:"
echo "  - Database: task_db"
echo "  - Username: task_user"
echo "  - Password: task_pass"
echo ""
echo "🔧 Useful Commands:"
echo "  - View status:         docker-compose ps"
echo "  - View logs:           docker-compose logs -f [app|broker|namesrv]"
echo "  - Stop all services:   docker-compose down"
echo "  - Restart app:         docker-compose restart app"
echo ""
echo "========================================="
