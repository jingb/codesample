# Task Async Service

基于 Spring Boot 3.2 + RocketMQ 5.3 的异步任务处理服务，演示通过固定消费线程池实现**恒定吞吐量控制**。

## 项目概述

这是一个处理长时任务的骨架/原型，演示如何使用固定消费线程池实现可预测的资源消耗。通过固定消费线程池，无论队列深度如何，都能实现恒定吞吐量。

**核心设计**: 固定线程池（10 个线程）→ 恒定资源消耗 → 可预测吞吐量（10 秒任务达到 1 任务/秒）

### 核心特性

- REST API 任务提交和状态查询
- 基于 RocketMQ 的异步处理
- 恒定吞吐量控制（固定消费线程池）
- 任务状态生命周期（PENDING → RUNNING → SUCCESS/FAILED）
- Docker Compose 依赖环境设置

---

## 快速开始

### 前置条件

- Docker & Docker Compose
- JDK 17+
- Maven

### 一键启动

```bash
./scripts/docker/run-all.sh
```

此命令将启动 RocketMQ、MySQL 和 Spring Boot 应用。

### 验证服务

```bash
# 健康检查
curl http://localhost:8080/health

# RocketMQ 控制台
open http://localhost:8081
```

---

## 常用命令

### 启动服务

```bash
# 启动完整环境（Docker + 应用）
./scripts/docker/run-all.sh

# 仅启动 Docker 服务
./scripts/docker/docker-start.sh

# 手动启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### 提交任务

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskType": "DATA_EXPORT",
    "params": {"userId": "123"}
  }'
```

### 查询任务状态

```bash
curl http://localhost:8080/tasks/{taskId}
```

### 运行压力测试

```bash
./scripts/testing/stress-test.sh
```

### 停止服务

```bash
./scripts/docker/docker-stop.sh
```

---

## 文档导航

- **[开发指南](docs/DEVELOPMENT.md)** - 开发环境配置、Docker 配置、故障排查
- **[测试指南](docs/TESTING.md)** - 压力测试参数、性能基准、结果分析
- **[架构说明](docs/ARCHITECTURE.md)** - 系统架构、恒定吞吐量机制、组件详解
- **[脚本参考](docs/SCRIPTS.md)** - 所有脚本文档和使用示例
- **[CLAUDE.md](CLAUDE.md)** - Claude Code 项目说明

---

## 项目结构

```
task-async-service/
├── docs/                          # 文档
│   ├── DEVELOPMENT.md             # 开发设置指南
│   ├── TESTING.md                 # 测试和性能
│   ├── ARCHITECTURE.md            # 系统架构
│   └── SCRIPTS.md                 # 脚本参考
├── scripts/                       # 自动化脚本
│   ├── docker/                    # Docker 管理
│   ├── testing/                   # 压力测试
│   └── monitoring/                # 监控工具
├── src/main/java/com/example/task/
│   ├── controller/                # REST 端点
│   ├── service/                   # 业务逻辑
│   ├── consumer/                  # RocketMQ 消费者
│   ├── entity/                    # 领域模型
│   └── config/                    # 配置 Bean
├── docker/                        # Docker 配置
├── docker-compose.yml             # 服务编排
└── pom.xml                        # Maven 构建
```

---

## 架构设计

### 请求流程

```
客户端 → POST /tasks → TaskService → RocketMQ → 消费者（固定 10 线程）→ TaskExecutionService
                                      ↓
                                    返回 taskId
```

### 恒定吞吐量机制

```java
consumer.setConsumeThreadMin(10);
consumer.setConsumeThreadMax(10);
```

**吞吐量公式**: `(3600 / 任务时长) × 线程数`

示例: `(3600 / 10) × 10 = 3600 任务/小时 = 1 任务/秒`

详见 [架构说明](docs/ARCHITECTURE.md)。

---

## API 接口

### POST /tasks
提交新任务进行异步处理。

**请求**:
```json
{
  "taskType": "DATA_EXPORT",
  "params": {
    "userId": "123"
  }
}
```

**响应**:
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "createdAt": "2026-01-18T10:00:00"
}
```

### GET /tasks/{taskId}
查询任务状态和详情。

**响应**:
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "DATA_EXPORT",
  "status": "RUNNING",
  "progress": 45,
  "createdAt": "2026-01-18T10:00:00",
  "startedAt": "2026-01-18T10:01:00"
}
```

---

## 技术栈

- **Java 17**
- **Spring Boot 3.2.0**
- **RocketMQ Client 5.3.0**（官方 SDK）
- **Maven**
- **Lombok**
- **Docker Compose**

---

## 骨架代码状态

**已实现**:
- REST API（提交/查询任务）
- RocketMQ 官方 SDK 集成
- 恒定吞吐量控制（固定线程池）
- 任务状态生命周期
- RocketMQ 自动背压
- Docker Compose 环境设置

**TODO**（标记为注释）:
- 数据库持久化（当前使用内存存储）
- Repository 层实现
- 单元测试
- 监控和指标
- 动态线程池大小调整

---

## 验证的性能指标

压力测试结果（100 个任务、10 个线程、10 秒任务）:

| 指标 | 理论值 | 实际值 | 状态 |
|------|--------|--------|------|
| 吞吐量 | 1.00 任务/秒 | 0.990 任务/秒 | ✅ |
| 完成时间 | 110 秒 | 101 秒 | ✅ |
| 最大并发数 | 10 | 10 | ✅ |

详见 [测试指南](docs/TESTING.md)。

---

## 许可证

MIT
