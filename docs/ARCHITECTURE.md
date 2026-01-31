# 架构文档

本文档描述系统架构、设计模式和恒定吞吐量控制机制。

---

## 目录

- [系统概述](#系统概述)
- [请求流程](#请求流程)
- [组件架构](#组件架构)
- [恒定吞吐量控制](#恒定吞吐量控制)
- [数据模型](#数据模型)
- [技术栈](#技术栈)

---

## 系统概述

Task Async Service 是一个基于 **Spring Boot 3.2 + RocketMQ 5.3** 的异步任务处理系统，演示通过固定消费线程池实现**恒定吞吐量控制**。

### 核心设计原则

**固定消费线程池 → 恒定资源消耗 → 可预测吞吐量**

使用 10 个线程处理 10 秒任务：
- 吞吐量：1 任务/秒（3600 任务/小时）
- 并发数：最大 10 个任务
- 资源使用：恒定，不受队列深度影响

### 高层架构

```
┌─────────────┐      ┌──────────────┐      ┌──────────┐      ┌─────────────┐
│   客户端    │─────▶│ REST API     │─────▶│ RocketMQ │─────▶│  消费者     │
│             │◀─────│ (Controller) │◀─────│   Queue   │◀─────│ (固定 10)   │
└─────────────┘      └──────────────┘      └──────────┘      └─────────────┘
                                                           │
                                                           ▼
                                                    ┌─────────────┐
                                                    │   任务      │
                                                    │  执行服务   │
                                                    └─────────────┘
```

---

## 请求流程

### 1. 任务提交

```
客户端 → POST /tasks → TaskController → TaskService → RocketMQ Producer
                                                           ↓
                                                    task-topic Queue
                                                           ↓
                                          立即返回 taskId
```

**执行顺序**：
1. 客户端通过 REST API 提交任务
2. Controller 验证请求
3. Service 创建任务实体，状态为 PENDING
4. Producer 发送轻量消息到 RocketMQ
5. Service 立即返回 taskId 给客户端

### 2. 任务处理

```
RocketMQ → TaskConsumer (固定 10 线程) → TaskExecutionService → 任务存储
             (当线程繁忙时                                   ↓
               自动暂停)                                更新状态
                                                          ↓
                                                    客户端查询状态
```

**执行顺序**：
1. RocketMQ 在消费者线程可用时推送消息
2. Consumer 更新任务状态为 RUNNING
3. ExecutionService 处理任务（模拟延迟）
4. Consumer 更新任务状态为 SUCCESS/FAILED
5. 手动 ACK 给 RocketMQ

### 3. 状态查询

```
客户端 → GET /tasks/{taskId} → TaskController → TaskService → 内存存储
                                                          ↓
                                                    返回任务详情
```

---

## 组件架构

### Controller 层 (`TaskController`)

**职责**：HTTP 请求处理、验证、响应格式化

**接口**：
- `POST /tasks` - 提交任务（创建任务、发送到 MQ、返回 taskId）
- `GET /tasks/{taskId}` - 查询任务状态和详情
- `GET /health` - 健康检查端点

**位置**：`src/main/java/com/example/task/controller/TaskController.java`

---

### Service 层

#### TaskService

**职责**：任务生命周期管理

**方法**：
- `createTask()` - 创建任务、生成 UUID、初始化状态
- `sendToQueue()` - 通过 Producer 发送消息到 RocketMQ
- `getTask()` - 从存储检索任务
- `updateTaskStatus()` - 更新任务状态和时间戳

**当前存储**：内存 `ConcurrentHashMap`（标记为 TODO，待实现数据库）

**位置**：`src/main/java/com/example/task/service/TaskService.java`

#### TaskExecutionService

**职责**：实际任务执行逻辑

**任务类型**：
- `DATA_EXPORT` - 数据导出操作
- `DATA_IMPORT` - 数据导入操作
- `REPORT_GENERATION` - 报表生成

**执行**：通过 `Thread.sleep()` 模拟延迟（可配置时长）

**位置**：`src/main/java/com/example/task/service/TaskExecutionService.java`

---

### Consumer 层 (`TaskConsumer`)

**职责**：从 RocketMQ 消费消息

**关键特性**：
- 使用 RocketMQ **官方 SDK**（无 Spring 注解）
- 固定线程池：`consumeThreadMin=10`、`consumeThreadMax=10`
- 手动 ACK/NACK：`CONSUME_SUCCESS` / `RECONSUME_LATER`
- 使用 `@PreDestroy` 优雅关闭

**消费逻辑**：
1. 从队列接收 `TaskMessage`
2. 从存储检索完整任务
3. 更新状态为 RUNNING
4. 委托给 `TaskExecutionService`
5. 更新状态为 SUCCESS/FAILED
6. 返回确认状态

**位置**：`src/main/java/com/example/task/consumer/TaskConsumer.java`

---

### Configuration 层 (`RocketMQConfig`)

**职责**：RocketMQ Producer 和 Consumer 配置

**Bean 定义**：
- `DefaultMQProducer` - 发送消息到 RocketMQ
- `DefaultMQPushConsumer` - 从 RocketMQ 接收消息

**配置**：
- NameServer 地址（来自 application.yml）
- Producer 组名
- Consumer 组名
- Topic 订阅
- 线程池设置
- 使用 `@PreDestroy` 优雅关闭

**位置**：`src/main/java/com/example/task/config/RocketMQConfig.java`

---

## 恒定吞吐量控制

### 机制

```java
// TaskConsumer.java
consumer.setConsumeThreadMin(10);
consumer.setConsumeThreadMax(10);
```

### 工作原理

**RocketMQ 的自动背压**：

1. **线程池满时**：RocketMQ 暂停消息推送
2. **线程释放后**：RocketMQ 恢复消息推送
3. **结果**：实际并发 = 线程池大小

### 吞吐量公式

```
吞吐量（任务/小时）= (3600 / 任务时长) × 线程数

示例：
  任务时长：10 秒
  线程数：10
  吞吐量 = (3600 / 10) × 10 = 3600 任务/小时 = 1 任务/秒
```

### 资源行为

**传统方法**（无界线程池）：
```
队列深度:  0 → 100 → 500 → 1000
并发数:   0 → 20  → 50  → 100  ← 无界增长
CPU 使用:  5% → 30% → 70% → 95%  ← 系统过载风险
```

**固定线程池方法**：
```
队列深度:  0 → 100 → 500 → 1000
并发数:   0 → 10  → 10  → 10   ← 限制在线程数
CPU 使用:  5% → 30% → 30% → 30%  ← 恒定、可预测
```

### 优势

1. **可预测资源使用**：CPU/内存保持恒定
2. **SLA 保证**：完成时间 = `(任务数 / 线程数) × 时长`
3. **突发流量处理**：队列吸收峰值而不超载
4. **简单性**：基于配置，无需复杂算法

---

## 数据模型

### Task 实体

**用途**：存储在内存/数据库中的完整任务状态

**字段**：
```java
String taskId;        // UUID
String taskType;      // DATA_EXPORT, DATA_IMPORT, REPORT_GENERATION
TaskStatus status;    // PENDING, RUNNING, SUCCESS, FAILED
Integer progress;     // 0-100
String result;        // 执行结果（JSON）
String errorMessage;  // 错误详情（如果失败）
Instant createdAt;    // 任务创建时间戳
Instant startedAt;    // 处理开始时间戳
Instant finishedAt;   // 处理完成时间戳
Map<String, Object> params;  // 任务参数
```

**状态生命周期**：
```
PENDING → RUNNING → SUCCESS
                   ↘ FAILED
```

**位置**：`src/main/java/com/example/task/entity/Task.java`

---

### TaskMessage

**用途**：RocketMQ 传输的轻量消息

**字段**：
```java
String taskId;      // 引用 Task 实体
String taskType;    // 任务类型枚举
int retryCount;     // 重试计数器，用于重新投递
```

**设计**：最小化负载以减少队列存储开销

**位置**：`src/main/java/com/example/task/entity/TaskMessage.java`

---

## 技术栈

| 组件 | 技术 | 版本 | 用途 |
|-----------|-----------|---------|---------|
| **应用框架** | Spring Boot | 3.2.0 | REST API、依赖注入 |
| **语言** | Java | 17 | 核心运行时 |
| **构建工具** | Maven | 3.6+ | 依赖管理、构建 |
| **消息队列** | RocketMQ Client | 5.3.0 | 异步消息传递 |
| **代码生成** | Lombok | Latest | 减少样板代码 |
| **数据库**（可选） | MySQL | 8.0 | 任务持久化（TODO） |
| **容器化** | Docker Compose | Latest | 依赖编排 |

---

## 配置文件

### 应用配置

| 文件 | Profile | NameServer | 用途 |
|------|---------|------------|---------|
| `application.yml` | default | localhost:9876 | 本地开发 |
| `application-docker.yml` | docker | namesrv:9876 | Docker 环境 |

### Docker 配置

| 文件 | 用途 |
|------|---------|
| `docker-compose.yml` | 服务编排 |
| `docker/rocketmq/broker.conf` | RocketMQ Broker 设置 |
| `docker/mysql/init.sql` | 数据库模式 |

---

## 包结构

```
com.example.task/
├── TaskAsyncServiceApplication.java  # 主入口（@SpringBootApplication）
├── config/
│   └── RocketMQConfig.java           # Producer/Consumer Bean
├── controller/
│   └── TaskController.java           # REST 端点
├── service/
│   ├── TaskService.java              # 任务生命周期管理
│   └── TaskExecutionService.java     # 任务执行逻辑
├── consumer/
│   └── TaskConsumer.java             # RocketMQ 消息监听器
├── entity/
│   ├── Task.java                     # 任务领域模型
│   └── TaskMessage.java              # 队列消息模型
└── repository/
    └── (空 - TODO 待实现数据库)
```

---

## 设计模式

### 使用的模式

1. **Builder 模式**：所有实体使用 Lombok `@Builder`
2. **Service 层模式**：业务逻辑与 Controller 分离
3. **消息队列模式**：通过 RocketMQ 异步解耦
4. **线程池模式**：固定池用于资源控制
5. **Repository 模式**：数据访问抽象（TODO）

### 代码约定

- **优雅关闭**：所有组件实现 `@PreDestroy`
- **手动 ACK**：RocketMQ 消费者使用显式确认
- **状态跟踪**：完整生命周期和时间戳
- **日志记录**：使用 SLF4J 和 Lombok 的 `@Slf4j`

---

## 可扩展性考虑

### 当前限制（骨架代码）

1. **单实例**：消费者在单个应用实例上运行
2. **内存存储**：任务数据在重启时丢失
3. **无监控**：指标未暴露（TODO）
4. **固定线程池**：无法动态调整容量

### 生产增强（TODO）

1. **水平扩展**：
   - 部署多个消费者实例
   - 每个实例维护固定线程池
   - 总吞吐量 = 实例数 × 单实例吞吐量

2. **数据库持久化**：
   - 使用 MySQL/PostgreSQL 替换 `ConcurrentHashMap`
   - 添加使用 JPA/Hibernate 的 Repository 层

3. **监控**：
   - Prometheus 指标（队列深度、处理速率）
   - Grafana 仪表板
   - 阈值告警

4. **动态线程池**：
   - 根据系统负载调整
   - 按任务类型区分池

---

## 相关文档

- [开发指南](DEVELOPMENT.md) - 设置和配置
- [测试指南](TESTING.md) - 性能验证
- [脚本参考](SCRIPTS.md) - 自动化脚本
- [主 README](../README.md) - 项目概述
