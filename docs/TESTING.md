# 测试指南

本指南涵盖压力测试、性能验证和结果分析，用于验证恒定吞吐量控制机制。

---

## 目录

- [概述](#概述)
- [测试配置](#测试配置)
- [理论计算](#理论计算)
- [运行测试](#运行测试)
- [分析结果](#分析结果)
- [性能基准](#性能基准)
- [RocketMQ Console 监控](#rocketmq-console-监控)

---

## 概述

本项目演示通过固定消费线程池实现**恒定吞吐量控制**。压力测试验证以下目标：

1. 并发数限制在线程池大小（10 个线程）
2. 吞吐量保持恒定，不受队列深度影响
3. 完成时间可预测：`(任务数 / 线程数) × 时长`

### 验证结果（截至 2026-01-25）

| 指标 | 理论值 | 实际值 | 误差 | 状态 |
|--------|-------------|--------|-------|--------|
| 吞吐量 | 1.00 任务/秒 | 0.990 任务/秒 | -1.0% | ✅ 通过 |
| 完成时间 | 110 秒 | 101 秒 | -8.1% | ✅ 通过 |
| 最大并发数 | 10 | 10 | 0% | ✅ 精确 |

---

## 测试配置

### 参数（默认值）

| 参数 | 值 | 来源 |
|-----------|-------|--------|
| **任务时长** | 10 秒/任务 | `application-docker.yml: task.processing.duration-seconds` |
| **线程池大小** | 10 线程 | `application-docker.yml: rocketmq.consumer.consume-thread-min` |
| **总任务数** | 100 个任务 | `scripts/testing/stress-test.sh: TOTAL_TASKS` |
| **任务类型** | DATA_EXPORT | 固定 |
| **提交方式** | 快速批量提交 | Shell 脚本循环 |

### 自定义参数

编辑 `scripts/testing/stress-test.sh`：

```bash
TOTAL_TASKS=200          # 修改任务数量
TASK_TYPE="DATA_IMPORT"  # 修改任务类型
BASE_URL="http://localhost:8080"  # API 端点
```

编辑 `src/main/resources/application-docker.yml`：

```yaml
task:
  processing:
    duration-seconds: 5  # 修改任务时长

rocketmq:
  consumer:
    consume-thread-min: 20  # 修改线程池
    consume-thread-max: 20
```

---

## 理论计算

### 吞吐量公式

```
吞吐量（任务/小时）= (3600 / 任务时长) × 线程数

示例：
  = (3600 / 10) × 10
  = 360 × 10
  = 3600 任务/小时
  = 60 任务/分钟
  = 1 任务/秒
```

### 完成时间公式

```
总时间 = (任务数 / 线程数) × 任务时长 + 启动延迟

示例（100 任务、10 线程、10 秒任务）：
  = (100 / 10) × 10 + 10
  = 10 × 10 + 10
  = 110 秒
```

### 并发行为预期

**预期曲线**：
```
活跃任务数
  12 |                                    （可能短暂达到 11-12）
  10 |    ████████████████████████████   （大部分时间在 10）
   8 |
   6 |
   4 |
   2 |
   0 |____________________________________
     0s   20s   40s   60s   80s   100s
```

---

## 运行测试

### 前置准备

```bash
# 1. 启动环境
./scripts/docker/run-all.sh

# 2. 等待 15 秒让服务就绪

# 3. 验证健康
curl http://localhost:8080/health
```

### 执行压力测试

```bash
./scripts/testing/stress-test.sh
```

**预期输出**：
```
Submitting 100 tasks...
Task 1/100 submitted: taskId=abc-123
Task 2/100 submitted: taskId=def-456
...
All 100 tasks submitted in 4.23 seconds
Rate: 23.64 tasks/second
```

### 实时监控（可选）

在另一个终端：

```bash
./scripts/monitoring/monitor-consumption.sh
```

或使用 RocketMQ Console：http://localhost:8081

---

## 分析结果

### 使用分析脚本

```bash
./scripts/testing/analyze-logs.sh stress-test-results/submissions_*.txt
```

**输出**：
```
=== 压力测试分析 ===
总任务数：100
持续时间：101 秒
吞吐量：0.990 任务/秒

并发度：
  最大活跃任务：10
  平均：9.5

时间线：
  首个任务开始：06:23:20
  最后任务完成：06:25:01
```

### 手动分析

1. **检查提交记录**：
   ```bash
   cat stress-test-results/submissions_*.txt
   ```

2. **查看应用日志**：
   ```bash
   docker logs task-app | grep -E "(Task started|Task executed|activeTasks)"
   ```

3. **验证 RocketMQ 统计**：
   ```bash
   ./scripts/monitoring/verify-rocketmq-stats.sh
   ```

---

## 性能基准

### 不同配置的吞吐量

| 线程数 | 任务时长 | 吞吐量（任务/小时） |
|--------------|---------------|-------------------------|
| 10 | 5 秒 | 7200 |
| 10 | 10 秒 | 3600 |
| 10 | 30 秒 | 1200 |
| 20 | 10 秒 | 7200 |
| 20 | 30 秒 | 2400 |

**公式**：`(3600 / 时长) × 线程数`

### 完成时间估算

| 任务数 | 线程数 | 时长（秒） | 预计时间（秒） |
|-------|---------|----------------|-----------------|
| 100 | 10 | 10 | 110 |
| 100 | 20 | 10 | 60 |
| 1000 | 10 | 10 | 1010 |
| 1000 | 20 | 10 | 510 |

---

## RocketMQ Console 监控

测试期间访问 http://localhost:8081 观察：

### 1. 消息积压

**路径**：消息 → 消息查询 → Topic: `task-topic`

**预期模式**：
```
积压量
  100 |●
  90 | ●
  80 |  ●
  70 |   ●
  60 |    ●
  50 |     ●
  40 |      ●
  30 |       ●
  20 |        ●
  10 |         ●
   0 |          ●________________________
     0s   20s   40s   60s   80s   100s  120s
```

### 2. 消费者组状态

**路径**：消费者 → 消费者组 → `task-consumer-group`

**关键指标**：
- **消费延迟**：先增大后减小
- **消费 TPS**：稳定在 ~60 消息/分钟
- **Rebalance**：不应频繁发生

### 3. 消息详情

**路径**：消息 → 消息查询 → Topic: `task-topic`

检查：
- 消息总数（应等于 `TOTAL_TASKS`）
- 消费状态（已消费/未消费）

---

## 验证检查清单

每次压力测试后，验证：

- [ ] 任务数等于 `TOTAL_TASKS`
- [ ] 最大并发 ≤ 线程池大小 + 2
- [ ] 完成时间在理论值的 ±20% 内
- [ ] 吞吐量在理论值的 ±20% 内
- [ ] RocketMQ Console 显示预期的积压模式
- [ ] 无任务失败或重试

---

## 为什么固定线程池 = 恒定吞吐？

### 机制

1. **线程池满时**：RocketMQ 暂停消息推送
2. **线程释放后**：RocketMQ 恢复消息推送
3. **结果**：实际并发 = 线程池大小

### 类比

就像一个餐厅有 **10 个服务员**：
- 即使有 100 个顾客等待
- 同时只有 10 个能被服务
- **服务速率** = 10 × (1 顾客 / 10 秒) = 1 顾客/秒（恒定）

### 生产价值

- **可预测资源使用**：CPU/内存保持恒定
- **突发流量处理**：队列吸收峰值而不超载
- **SLA 保证**：完成时间可计算
- **简单可靠**：无需复杂的限流算法

---

## 故障排查

### 测试失败

**症状**：任务未完成
**检查**：
```bash
# 验证应用正在运行
curl http://localhost:8080/health

# 检查消费者是否注册
./scripts/monitoring/verify-rocketmq-stats.sh
```

**症状**：吞吐量低于预期
**检查**：
- 任务时长可能长于配置值
- 线程池可能未达到 min/max
- 系统资源竞争

### 日志分析

如果分析脚本失败：

```bash
# 手动检查日志
docker logs task-app > stress-test-results/docker_logs_manual.txt

# 搜索关键事件
grep "Task started" stress-test-results/docker_logs_manual.txt | wc -l
grep "Task executed" stress-test-results/docker_logs_manual.txt | wc -l
grep "activeTasks" stress-test-results/docker_logs_manual.txt | sort -u
```

---

## 相关文件

| 文件 | 用途 |
|------|---------|
| `scripts/testing/stress-test.sh` | 执行压力测试 |
| `scripts/testing/analyze-logs.sh` | 分析结果 |
| `scripts/monitoring/monitor-consumption.sh` | 实时监控 |
| `scripts/monitoring/verify-rocketmq-stats.sh` | 验证 RocketMQ 统计 |
| `stress-test-results/submissions_*.txt` | 任务提交记录 |
| `stress-test-results/docker_logs_*.txt` | 完整日志 |
| `stress-test-results/analysis_*.txt` | 分析报告 |

---

## 相关文档

- [脚本参考](SCRIPTS.md) - 脚本使用详情
- [开发指南](DEVELOPMENT.md) - 环境设置
- [架构说明](ARCHITECTURE.md) - 吞吐量控制机制
