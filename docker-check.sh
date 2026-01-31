#!/bin/bash

# Docker Environment Verification Script
# Checks if all services are running and accessible

echo "========================================="
echo "Checking Docker Environment Status..."
echo "========================================="
echo ""

# Check container status
echo "📦 Container Status:"
docker-compose ps
echo ""

# Check NameServer
echo "🔍 Checking RocketMQ NameServer (localhost:9876)..."
if nc -zv localhost 9876 2>&1 | grep -q "succeeded"; then
    echo "   ✅ NameServer is reachable"
else
    echo "   ❌ NameServer is NOT reachable"
fi
echo ""

# Check Broker
echo "🔍 Checking RocketMQ Broker (localhost:10911)..."
if nc -zv localhost 10911 2>&1 | grep -q "succeeded"; then
    echo "   ✅ Broker is reachable"
else
    echo "   ❌ Broker is NOT reachable"
    echo "   (Broker may still be starting, wait 30s and retry)"
fi
echo ""

# Check Console
echo "🔍 Checking RocketMQ Console (http://localhost:8081)..."
if curl -s http://localhost:8081 > /dev/null 2>&1; then
    echo "   ✅ Console is accessible"
else
    echo "   ⏳ Console may still be starting"
fi
echo ""

# Check MySQL
echo "🔍 Checking MySQL (localhost:3306)..."
if nc -zv localhost 3306 2>&1 | grep -q "succeeded"; then
    echo "   ✅ MySQL is reachable"
else
    echo "   ❌ MySQL is NOT reachable"
fi
echo ""

echo "========================================="
echo "✅ Verification Complete!"
echo "========================================="
echo ""
echo "📋 Service URLs:"
echo "  - RocketMQ NameServer: localhost:9876"
echo "  - RocketMQ Broker:     localhost:10911"
echo "  - RocketMQ Console:    http://localhost:8081"
echo "  - MySQL Database:      localhost:3306"
echo ""
echo "💡 Next Steps:"
echo "  1. Open Console: open http://localhost:8081"
echo "  2. Run application: mvn spring-boot:run -Dspring-boot.run.profiles=docker"
echo "  3. Test API: curl -X POST http://localhost:8080/tasks -H 'Content-Type: application/json' -d '{\"taskType\":\"DATA_EXPORT\",\"params\":{}}'"
echo "========================================="
