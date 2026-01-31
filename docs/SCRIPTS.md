# 脚本参考

项目中的所有脚本按功能分类存放在 `scripts/` 目录下的三个子目录中。

---

## Docker 管理脚本 (`scripts/docker/`)

### docker-start.sh
启动 Docker Compose 服务（RocketMQ + MySQL + Console）。

```bash
./scripts/docker/docker-start.sh
```

**功能说明**：
- 启动 `docker-compose.yml` 中定义的所有服务
- 等待服务健康检查通过
- 显示服务状态和访问地址

**输出示例**：
```
Starting Docker services...
Service status: https://localhost:8081
```

---

### docker-stop.sh
停止所有 Docker 服务但保留数据。

```bash
./scripts/docker/docker-stop.sh
```

**功能说明**：
- 优雅地停止所有容器
- 保留数据卷（数据不会删除）

---

### docker-check.sh
检查所有 Docker 服务的健康状态。

```bash
./scripts/docker/docker-check.sh
```

**功能说明**：
- 显示容器状态（运行中/已停止）
- 显示健康检查结果
- 列出暴露的端口

---

### run-all.sh
启动完整环境：Docker 服务 + Spring Boot 应用。

```bash
./scripts/docker/run-all.sh
```

**功能说明**：
1. 启动 Docker 服务（调用 `docker-start.sh`）
2. 如需要则构建应用（检测源码变化）
3. 启动 Spring Boot 应用并使用 `docker` profile
4. 前台运行（按 Ctrl+C 停止）

**注意**：这是推荐的一键启动完整环境的方式。

---

## 测试脚本 (`scripts/testing/`)

### stress-test.sh
提交一批任务以测试系统吞吐量和并发控制能力。

```bash
./scripts/testing/stress-test.sh
```

**配置**（编辑脚本修改）：
```bash
TOTAL_TASKS=100          # 提交任务数量
TASK_TYPE="DATA_EXPORT"  # 任务类型
BASE_URL="http://localhost:8080"  # API 端点
```

**功能说明**：
- 尽可能快地提交 100 个任务
- 将提交记录保存到 `stress-test-results/submissions_*.txt`
- 记录时间戳用于延迟分析

**输出示例**：
```
Submitted 100 tasks in 4.23 seconds
Rate: 23.64 tasks/second
```

---

### analyze-logs.sh
分析压力测试日志以计算吞吐量和并发指标。

```bash
./scripts/testing/analyze-logs.sh stress-test-results/submissions_*.txt
```

**功能说明**：
- 解析应用日志中的任务执行事件
- 计算实际吞吐量（任务/秒）
- 确定最大并发任务数
- 生成分析报告

**输出示例**：
```
Total tasks: 100
Duration: 101 seconds
Throughput: 0.990 tasks/second
Max concurrent tasks: 10
```

---

## 监控脚本 (`scripts/monitoring/`)

### monitor-consumption.sh
实时监控 RocketMQ 消费指标。

```bash
./scripts/monitoring/monitor-consumption.sh
```

**功能说明**：
- 持续监控任务消费速率
- 显示活跃任务数
- 显示队列积压情况
- 每 2 秒更新一次

**输出示例**（实时）：
```
Time: 14:23:45 | Active: 10 | Consumed: 45 | Pending: 55
Time: 14:23:47 | Active: 10 | Consumed: 47 | Pending: 53
...
```

按 Ctrl+C 停止监控。

---

### verify-rocketmq-stats.sh
通过 Console API 验证 RocketMQ 统计信息。

```bash
./scripts/monitoring/verify-rocketmq-stats.sh
```

**功能说明**：
- 查询 RocketMQ Console 的 topic 统计
- 显示消息总数、已消费数、待处理数
- 显示消费者组状态

**输出示例**：
```
Topic: task-topic
Total Messages: 100
Consumed: 98
Pending: 2
Consumer Group: task-consumer-group
Status: OK
```

---

## 常用工作流

### 开发工作流
```bash
# 1. 启动依赖服务
./scripts/docker/docker-start.sh

# 2. 启动应用（另一个终端）
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 3. 手动测试
curl -X POST http://localhost:8080/tasks -H "Content-Type: application/json" \
  -d '{"taskType": "DATA_EXPORT", "params": {"userId": "123"}}'
```

### 快速启动（一体化）
```bash
# 启动所有服务
./scripts/docker/run-all.sh
```

### 压力测试工作流
```bash
# 1. 启动环境
./scripts/docker/run-all.sh

# 2. 等待 15 秒让服务就绪

# 3. 运行压力测试（另一个终端）
./scripts/testing/stress-test.sh

# 4. 实时监控（可选，第三个终端）
./scripts/monitoring/monitor-consumption.sh

# 5. 分析结果
./scripts/testing/analyze-logs.sh stress-test-results/submissions_*.txt
```

### 验证工作流
```bash
# 1. 检查服务健康
./scripts/docker/docker-check.sh

# 2. 验证 RocketMQ 统计
./scripts/monitoring/verify-rocketmq-stats.sh

# 3. 查看日志
docker-compose logs -f broker
```

### 清理工作流
```bash
# 停止服务（保留数据）
./scripts/docker/docker-stop.sh

# 停止并删除所有数据（警告：破坏性操作）
docker-compose down -v
```

---

## 脚本退出码

所有脚本使用标准退出码：
- `0` - 成功
- `1` - 一般错误
- `2` - 使用错误（参数无效）

---

## 故障排查

### 找不到脚本
确保从项目根目录运行：
```bash
cd /path/to/task-async-service
./scripts/docker/docker-start.sh
```

### 权限被拒绝
为脚本添加可执行权限：
```bash
chmod +x scripts/**/*.sh
```

### 服务无法启动
检查 Docker 是否运行：
```bash
docker ps
docker-compose logs
```

---

## 相关文档

- [开发指南](DEVELOPMENT.md) - 开发环境设置
- [测试指南](TESTING.md) - 压力测试详情
- [架构说明](ARCHITECTURE.md) - 系统架构概览
