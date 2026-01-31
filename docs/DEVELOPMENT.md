# 开发指南

本指南涵盖开发环境设置和基于 Docker 的基础设施使用。

---

## 目录

- [前置条件](#前置条件)
- [快速开始](#快速开始)
- [Docker 环境](#docker-环境)
- [运行应用](#运行应用)
- [测试 API](#测试-api)
- [故障排查](#故障排查)
- [性能调优](#性能调优)

---

## 前置条件

确保已安装以下工具：

| 工具 | 版本 | 检查命令 |
|------|---------|---------------|
| **Docker** | 最新 | `docker --version` |
| **Docker Compose** | 最新 | `docker-compose --version` |
| **JDK** | 17+ | `java -version` |
| **Maven** | 3.6+ | `mvn -version` |

**端口可用性**：确保以下端口未被占用：
- `9876` - RocketMQ NameServer
- `10909`, `10911`, `10912` - RocketMQ Broker
- `8081` - RocketMQ Console
- `3306` - MySQL
- `8080` - Spring Boot Application

---

## 快速开始

### 一键启动（推荐）

```bash
./scripts/docker/run-all.sh
```

这将：
1. 启动所有 Docker 服务
2. 如需要则构建应用
3. 启动 Spring Boot 应用

### 分步启动

```bash
# 1. 启动 Docker 服务
./scripts/docker/docker-start.sh

# 2. 等待 15 秒让服务就绪

# 3. 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 或直接运行 JAR
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
```

### 验证服务

```bash
# 检查 Docker 服务状态
./scripts/docker/docker-check.sh

# 健康检查
curl http://localhost:8080/health

# 访问 RocketMQ Console
open http://localhost:8081
```

---

## Docker 环境

### 服务概览

| 服务 | 容器名 | 端口 | 用途 |
|---------|---------------|-------|---------|
| **RocketMQ NameServer** | rocketmq-namesrv | 9876 | 消息路由 |
| **RocketMQ Broker** | rocketmq-broker | 10909, 10911, 10912 | 消息存储 |
| **RocketMQ Console** | rocketmq-console | 8081 | Web UI |
| **MySQL** | task-mysql | 3306 | 数据库（可选） |

### Docker Compose 配置

`docker-compose.yml` 文件定义了：
- **网络隔离**：服务通过 `rocketmq_network` 通信
- **健康检查**：自动依赖管理
- **卷持久化**：数据在容器重启后保留
- **环境变量**：通过 `.env` 文件配置

### Profile 配置

- **default** (`application.yml`)：本地开发（localhost:9876）
- **docker** (`application-docker.yml`)：Docker 环境（namesrv:9876）

---

## 运行应用

### 开发模式（Maven）

```bash
# 使用 Docker profile
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 或使用环境变量
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

### 生产模式（JAR）

```bash
# 构建
mvn clean package -DskipTests

# 运行（使用 Docker profile）
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
```

### 停止服务

```bash
# 停止 Docker 服务（保留数据）
./scripts/docker/docker-stop.sh

# 或直接停止
docker-compose down

# 停止并删除所有数据（警告：破坏性操作）
docker-compose down -v
```

---

## 测试 API

### 1. 提交任务

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskType": "DATA_EXPORT",
    "params": {
      "userId": "123"
    }
  }'
```

**响应**：
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "createdAt": "2026-01-18T19:30:00"
}
```

### 2. 查询任务状态

```bash
curl http://localhost:8080/tasks/{taskId}
```

**响应**：
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "taskType": "DATA_EXPORT",
  "status": "RUNNING",
  "progress": 45,
  "result": null,
  "errorMessage": null,
  "createdAt": "2026-01-18T10:00:00",
  "startedAt": "2026-01-18T10:01:00",
  "finishedAt": null
}
```

### 3. RocketMQ Console

访问 http://localhost:8081 查看：
- Topic: `task-topic`
- 消息数量和消费者状态
- Broker 性能指标

---

## 故障排查

### 容器启动失败

**检查日志**：
```bash
docker-compose logs broker
docker-compose logs namesrv
```

**常见原因**：
- 端口被占用（9876, 10911, 8081, 3306）
- Docker 磁盘空间不足

### 应用无法连接 RocketMQ

**测试连接**：
```bash
# 测试 NameServer 是否可达
nc -zv localhost 9876

# 测试 Broker 是否可达
nc -zv localhost 10911
```

**解决方法**：
- 验证容器正在运行：`docker-compose ps`
- 等待服务完全启动（约 15 秒）
- 检查防火墙设置

### 消费者无法处理消息

**检查步骤**：
1. 访问 Console：http://localhost:8081
2. 查看 Topic 是否有消息
3. 查看消费者组状态
4. 检查应用日志

---

## 性能调优

### 调整 RocketMQ 内存

编辑 `docker-compose.yml`：
```yaml
environment:
  - JAVA_OPT_EXT=-Xms1G -Xmx1G -Xmn256m  # 增加内存
```

### 调整并发线程数

编辑 `application-docker.yml`：
```yaml
rocketmq:
  consumer:
    consume-thread-min: 20  # 增加线程数
    consume-thread-max: 20
```

**吞吐量公式**：`(3600 / 任务时长) × 线程数`

示例：`(3600 / 10) × 20 = 7200 任务/小时`

---

## 默认凭据

### MySQL

| 属性 | 值 |
|----------|-------|
| Host | localhost:3306 |
| Database | task_db |
| Username | task_user |
| Password | task_pass |
| Root Password | root123 |

**连接示例**：
```bash
mysql -h localhost -P 3306 -u task_user -ptask_pass task_db
```

---

## 目录结构

```
task-async-service/
├── docker-compose.yml          # Docker Compose 配置
├── .env                        # 环境变量
├── scripts/                    # 所有脚本
│   ├── docker/                 # Docker 管理脚本
│   ├── testing/                # 测试脚本
│   └── monitoring/             # 监控脚本
├── docker/
│   ├── rocketmq/
│   │   └── broker.conf        # Broker 配置
│   └── mysql/
│       └── init.sql           # MySQL 初始化脚本
└── src/main/resources/
    └── application-docker.yml  # Docker profile 配置
```

---

## 最佳实践

1. **开发时**：使用 Docker Compose 启动依赖，应用在本地运行
2. **测试时**：所有服务（包括应用）都在 Docker 中运行
3. **清理**：定期运行 `docker-compose down -v` 清理数据
4. **监控**：使用 `docker-compose logs -f` 查看实时日志
5. **端口冲突**：启动前停止使用相同端口的其他服务

---

## 学习目标

使用此环境可以学到：

1. **Docker Compose 基础**
   - 定义多服务应用
   - 依赖管理（`depends_on`）
   - 健康检查

2. **服务编排**
   - RocketMQ NameServer + Broker 配置
   - 网络隔离
   - 数据持久化

3. **环境隔离**
   - 开发环境 vs Docker 环境
   - Spring Profile 配置
   - 环境变量管理

---

## 相关文档

- [脚本参考](SCRIPTS.md) - 所有脚本文档
- [测试指南](TESTING.md) - 压力测试详情
- [架构说明](ARCHITECTURE.md) - 系统架构概览
- [主 README](../README.md) - 项目概述
