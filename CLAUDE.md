# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a **Spring Boot 3.2 + Java 17 + RocketMQ 5.3** asynchronous task processing service demonstrating **constant throughput control** through fixed consumer thread pools. It's a skeleton/prototype for handling long-running tasks with predictable resource consumption.

**Core Architecture**: REST API → Task Service → RocketMQ → Consumer (fixed thread pool) → Execution Service

**Key Design Principle**: Fixed consumer thread pool (10 threads) → constant resource usage → predictable throughput (1 task/sec with 10-second tasks)

---

## Build and Run Commands

### Build
```bash
# Clean build (skip tests - no tests exist yet)
mvn clean package -DskipTests

# Quick build
mvn package
```

### Run Application
```bash
# Using Maven (development mode)
mvn spring-boot:run

# Using Maven with Docker profile (when using Docker Compose for dependencies)
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# Using JAR file
java -jar target/task-async-service-1.0.0.jar

# Using JAR with Docker profile
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
```

### Docker Environment (for RocketMQ dependencies)
```bash
# Start all dependencies (RocketMQ + MySQL)
./scripts/docker/docker-start.sh
# or
docker-compose up -d

# Start complete environment (Docker + app)
./scripts/docker/run-all.sh

# Stop Docker services
./scripts/docker/docker-stop.sh
# or
docker-compose down

# Check service status
docker-compose ps
./scripts/docker/docker-check.sh

# View logs
docker-compose logs -f
docker-compose logs -f broker  # specific service
```

### Testing and Monitoring
```bash
# Stress test (100 tasks, 10 threads)
./scripts/testing/stress-test.sh

# Monitor consumption
./scripts/monitoring/monitor-consumption.sh

# Verify RocketMQ stats
./scripts/monitoring/verify-rocketmq-stats.sh

# Analyze logs
./scripts/testing/analyze-logs.sh
```

### API Testing
```bash
# Submit a task
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"taskType": "DATA_EXPORT", "params": {"userId": "123"}}'

# Query task status
curl http://localhost:8080/tasks/{taskId}

# Health check
curl http://localhost:8080/health
```

---

## Code Architecture

### Request Flow
```
Client → POST /tasks → TaskController → TaskService → RocketMQ Producer
                                                          ↓
                                                        Queue
                                                          ↓
TaskConsumer (fixed 10 threads) → TaskExecutionService → Task Storage
                                                          ↓
Client ← GET /tasks/{taskId} ← TaskController ← Task Status Update
```

### Key Components

**Controller Layer** (`TaskController`)
- `POST /tasks` - Submit task (creates task, sends to MQ, returns taskId)
- `GET /tasks/{taskId}` - Query task status and details
- `GET /health` - Health check endpoint

**Service Layer**
- **`TaskService`** - Task lifecycle management (create, send to MQ, update status)
- **`TaskExecutionService`** - Actual task execution (DATA_EXPORT, DATA_IMPORT, REPORT_GENERATION)

**Consumer Layer** (`TaskConsumer`)
- Uses RocketMQ **Official SDK** (no Spring annotations)
- Fixed thread pool: `consumeThreadMin=10`, `consumeThreadMax=10`
- Manual ACK/NACK: `CONSUME_SUCCESS` / `RECONSUME_LATER`
- Graceful shutdown with `@PreDestroy`

**Configuration** (`RocketMQConfig`)
- Creates and starts `DefaultMQProducer`
- Configures NameServer address
- Graceful shutdown with `@PreDestroy`

**Entity Layer**
- **`Task`** - Task entity with status lifecycle (PENDING → RUNNING → SUCCESS/FAILED)
- **`TaskMessage`** - Lightweight message for RocketMQ (taskId, taskType, retryCount)

### Constant Throughput Mechanism

The core design pattern is **fixed consumer thread pool** for predictable resource usage:

```java
consumer.setConsumeThreadMin(10);
consumer.setConsumeThreadMax(10);
```

**How it works**:
- Thread pool size = 10 threads (fixed)
- Each task takes ~10 seconds (configurable via `task.processing.duration-seconds`)
- RocketMQ's automatic backpressure pauses message push when threads are busy
- Resource consumption stays constant regardless of queue depth

**Throughput Formula**: `Tasks/Hour = (3600 / TaskDuration) × ThreadCount`
- Example: (3600 / 10) × 10 = 3600 tasks/hour = 1 task/second

---

## Configuration Files

- **`application.yml`** - Default configuration (localhost:9876)
- **`application-docker.yml`** - Docker environment (namesrv:9876)
- **`pom.xml`** - Maven build (Java 17, Spring Boot 3.2.0, RocketMQ 5.3.0)
- **`docker-compose.yml`** - Full stack (NameServer, Broker, Console, MySQL)
- **`Dockerfile`** - Multi-stage build with Amazon Corretto 17
- **`docker/rocketmq/broker.conf`** - Broker settings (auto-create topics, retry levels)
- **`docker/mysql/init.sql`** - MySQL schema (tasks table, indexes)

---

## Important Notes

### Current Implementation Status

**Implemented**:
- REST API (submit/query tasks)
- RocketMQ integration with official SDK
- Constant throughput control (fixed thread pool)
- Task status lifecycle
- Automatic backpressure via RocketMQ
- Docker Compose setup for dependencies

**Skeleton/TODO** (marked with TODO comments):
- Database persistence (currently using `ConcurrentHashMap` in `TaskService`)
- Repository layer implementation
- Unit tests (framework included but no tests written)
- Monitoring and metrics (Prometheus/Grafana)
- Dynamic thread pool sizing

### Code Patterns

- **Builder Pattern**: All entities use Lombok `@Builder`
- **Graceful Shutdown**: All components implement `@PreDestroy`
- **Manual ACK**: RocketMQ consumer uses explicit acknowledgment
- **Status Tracking**: Task lifecycle with timestamps (createdAt, startedAt, finishedAt)
- **Logging**: Uses SLF4J with Lombok's `@Slf4j`

### Package Structure
```
com.example.task/
├── TaskAsyncServiceApplication.java  # Main entry point
├── config/                           # Configuration beans
├── controller/                       # REST endpoints
├── service/                          # Business logic
├── consumer/                         # Message consumers
├── entity/                           # Domain models
└── repository/                       # Data access (empty - TODO)
```

---

## Testing

**Current Status**: No unit tests implemented yet. The `spring-boot-starter-test` dependency is included, but no `src/test` directory exists.

**Stress Testing**: Custom `stress-test.sh` script validates constant throughput control (100 tasks, 10 threads). Results saved to `stress-test-results/` directory. Validated throughput: 0.990 tasks/sec (theoretical: 1.00).

---

## Environment Profiles

- **default** (`application.yml`) - Local development (localhost:9876)
- **docker** (`application-docker.yml`) - Docker environment (namesrv:9876)

---

## Linting and Formatting

**No linting or formatting configured**. Consider adding:
- Checkstyle for Java code style
- SpotBugs for static analysis
- Google Java Format for consistent formatting

---

## RocketMQ Console

Web UI available at `http://localhost:8081` when using Docker Compose. View:
- Topic: `task-topic`
- Message count and consumer status
- Broker performance metrics

---

## Development Workflow

1. Start dependencies: `./scripts/docker/docker-start.sh` (wait 15 seconds for services)
2. Build application: `mvn clean package`
3. Run application: `mvn spring-boot:run -Dspring-boot.run.profiles=docker`
4. Test: `curl -X POST http://localhost:8080/tasks ...`
5. Monitor: `docker-compose logs -f` or access Console at http://localhost:8081

## Additional Documentation

- **[Development Guide](docs/DEVELOPMENT.md)** - Detailed development environment setup
- **[Testing Guide](docs/TESTING.md)** - Stress testing and performance analysis
- **[Architecture](docs/ARCHITECTURE.md)** - System architecture and component details
- **[Scripts Reference](docs/SCRIPTS.md)** - All scripts documentation

---

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- RocketMQ Client 5.3.0 (Official SDK)
- Maven
- Lombok
- Docker Compose (for dependencies)
