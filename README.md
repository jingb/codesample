# Task Async Service

> 最小化异步任务处理服务（骨架代码）

## 项目概述

这是一个基于 Spring Boot + RocketMQ 的异步任务处理服务，用于演示长时任务的恒定速率处理模式。

### 核心特性

- ✅ 两个 REST 接口：提交任务 / 查询任务
- ✅ 任务落库（TODO 注释）
- ✅ RocketMQ 异步解耦
- ✅ 恒定吞吐量控制（固定消费线程池）
- ✅ 任务状态流转（PENDING → RUNNING → SUCCESS/FAILED）
- ✅ 使用 RocketMQ 官方 SDK（无注解）

---

## 项目结构

```
task-async-service/
├── src/main/java/com/example/task/
│   ├── TaskAsyncServiceApplication.java  # 主应用类
│   ├── controller/
│   │   └── TaskController.java           # 两个接口
│   ├── service/
│   │   ├── TaskService.java              # 任务管理服务
│   │   └── TaskExecutor.java             # 实际任务执行逻辑
│   ├── consumer/
│   │   └── TaskConsumer.java             # RocketMQ 消费者（官方 SDK）
│   ├── entity/
│   │   ├── Task.java                     # 任务实体
│   │   └── TaskMessage.java              # MQ 消息体
│   └── config/
│       └── RocketMQConfig.java           # 配置类
├── src/main/resources/
│   └── application.yml                   # 配置文件
└── pom.xml                               # Maven 配置
```

---

## 接口设计

### 1. 提交任务

**Request:**
```http
POST /tasks
Content-Type: application/json

{
  "taskType": "DATA_EXPORT",
  "params": {
    "userId": "123",
    "dateRange": "2024-01-01:2024-12-31"
  }
}
```

**Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "createdAt": "2026-01-18T10:00:00"
}
```

### 2. 查询任务

**Request:**
```http
GET /tasks/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
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

---

## 核心设计：恒定吞吐量控制

### 关键代码

```java
// TaskConsumer.java
consumer.setConsumeThreadMin(10);
consumer.setConsumeThreadMax(10);
```

### 原理

1. **固定消费线程池**：RocketMQ 消费者的线程数固定为 10
2. **自动背压**：当线程池满时，RocketMQ 自动暂停推送
3. **资源可控**：并发数固定 → 资源消耗恒定 → 吞吐量恒定

### 计算公式

```
每小时任务数 = (3600秒 / 平均任务处理时间) × 线程数

例如：
- 平均任务处理时间：30秒
- 线程数：10
- 吞吐量：(3600 / 30) × 10 = 1200 任务/小时
```

---

## 运行方式

### 前置条件

1. 安装 JDK 17+
2. 安装 Maven
3. 启动 RocketMQ（默认 localhost:9876）

### 启动步骤

```bash
# 1. 进入项目目录
cd experiments/phase1-tool-calling/task-async-service

# 2. 编译项目
mvn clean package

# 3. 运行服务
java -jar target/task-async-service-1.0.0.jar

# 或直接用 Maven 运行
mvn spring-boot:run
```

### 测试接口

```bash
# 1. 提交任务
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskType": "DATA_EXPORT",
    "params": {
      "userId": "123"
    }
  }'

# 2. 查询任务（替换实际的 taskId）
curl http://localhost:8080/tasks/{taskId}
```

---

## 待完善（骨架代码说明）

以下部分已用 TODO 注释标记，需要根据实际需求实现：

### 1. 数据库持久化
- [ ] 创建 tasks 表
- [ ] 实现 TaskRepository
- [ ] 替换 TaskService 中的 ConcurrentHashMap

### 2. 监控和告警
- [ ] 添加 metrics（队列长度、处理速率、失败率）
- [ ] 集成 Prometheus/Grafana
- [ ] 配置告警规则（积压超过阈值）

### 3. 速率控制优化
- [ ] 按任务类型区分速率
- [ ] 动态调整线程池大小
- [ ] 更精细的限流策略

### 4. 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 压力测试

---

## 技术栈

- **Spring Boot 3.2.0**
- **RocketMQ 5.3.0**（官方 SDK）
- **Java 17**
- **Maven**
- **Lombok**

---

## 学习要点

### 1. RocketMQ 官方 SDK 使用
- ✅ 如何创建 `DefaultMQPushConsumer`
- ✅ 如何订阅 Topic 和 Tag
- ✅ 如何注册 `MessageListener`
- ✅ 如何手动 ACK（CONSUME_SUCCESS / RECONSUME_LATER）
- ✅ 如何优雅关闭

### 2. 恒定吞吐量实现
- ✅ 通过固定消费线程池控制并发
- ✅ RocketMQ 的自动背压机制
- ✅ 资源利用率保持在合理水位

### 3. 异步任务模式
- ✅ 任务提交 + 落库 + 返回 ID
- ✅ 消息队列解耦
- ✅ 状态查询接口
- ✅ 失败重试机制

---

