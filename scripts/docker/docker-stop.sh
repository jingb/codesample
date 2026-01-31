#!/bin/bash

# Docker Environment Shutdown Script
# This script stops all services and cleans up

set -e

echo "========================================="
echo "Stopping Docker Environment..."
echo "========================================="

# Stop services
echo "🛑 Stopping all services..."
docker-compose down

echo ""
echo "========================================="
echo "✅ All services stopped successfully!"
echo "========================================="
echo ""
echo "💡 To remove volumes as well (DELETE DATA):"
echo "   docker-compose down -v"
echo ""
echo "💡 To restart services:"
echo "   ./docker-start.sh"
echo "========================================="
