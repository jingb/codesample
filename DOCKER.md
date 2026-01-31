# Docker 环境使用指南

> 使用 Docker Compose 创建完全隔离的开发环境，不影响本机

---

## 🎯 优势

- ✅ **依赖隔离**：RocketMQ + MySQL 运行在 Docker 中，不影响本机环境
- ✅ **无需镜像**：应用使用本地 Java 运行，不拉取 Docker 镜像
- ✅ **节省资源**：只运行必要的服务，不浪费流量
- ✅ **一键启动**：单个命令启动完整环境
- ✅ **易于调试**：应用在本地运行，方便调试和日志查看

---

## 📋 包含的服务

| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| **RocketMQ NameServer** | rocketmq-namesrv | 9876 | 消息队列命名服务 |
| **RocketMQ Broker** | rocketmq-broker | 10909, 10911, 10912 | 消息队列代理 |
| **RocketMQ Console** | rocketmq-console | 8081 | Web 管理界面 |
| **MySQL** | task-mysql | 3306 | 数据库（可选） |

---

## 🚀 快速开始

### 前置条件

确保已安装：
- ✅ Docker（已安装并运行）
- ✅ Docker Compose
- ✅ JDK 17+
- ✅ Maven

### 启动步骤

#### 方式 A：一键启动所有服务（推荐）⭐

```bash
# 启动 Docker 服务 + Spring Boot 应用
./run-all.sh
```

**优势**：
- ✅ 一条命令启动完整环境
- ✅ 自动检测是否需要重新编译
- ✅ 应用在本地运行，使用本地 Java
- ✅ 无需拉取任何 Docker 镜像

---

#### 方式 B：分步启动

**步骤 1：启动 Docker 环境**

```bash
# 使用脚本（推荐）
./docker-start.sh

# 或直接使用 docker-compose
docker-compose up -d
```

**步骤 2：查看服务状态**

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f broker
```

**步骤 3：运行应用**

```bash
# 使用 Maven
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 或使用打包好的 JAR
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
```

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f broker
```

#### 3. 等待服务就绪

大约需要 10-15 秒，等待以下服务启动：
- ✅ NameServer 在 `localhost:9876`
- ✅ Broker 在 `localhost:10911`
- ✅ Console 在 `http://localhost:8081`

#### 4. 访问管理界面

打开浏览器访问：
- **RocketMQ Console**: http://localhost:8081

---

## 🏃 运行应用

### 方式 A：使用 Maven（开发模式）

```bash
# 使用 Docker profile
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# 或者设置环境变量
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

### 方式 B：打包后运行

```bash
# 打包
mvn clean package

# 运行（使用 Docker profile）
java -jar target/task-async-service-1.0.0.jar --spring.profiles.active=docker
```

---

## 🧪 测试接口

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

### 2. 查询任务

```bash
curl http://localhost:8080/tasks/{taskId}
```

### 3. 在 Console 查看消息

访问 http://localhost:8081，查看：
- Topic: `task-topic`
- 消息数量
- 消费者状态

---

## 🛑 停止服务

### 停止但保留数据

```bash
# 方式 A：使用脚本
./docker-stop.sh

# 方式 B：直接停止
docker-compose down
```

### 停止并删除所有数据（清理）

```bash
# ⚠️ 警告：会删除所有数据！
docker-compose down -v
```

---

## 🔍 故障排查

### 问题 1：容器启动失败

**检查日志**：
```bash
docker-compose logs broker
docker-compose logs namesrv
```

**常见原因**：
- 端口被占用（9876, 10911, 8081, 3306）
- Docker 磁盘空间不足

### 问题 2：应用无法连接 RocketMQ

**检查连接**：
```bash
# 测试 NameServer 是否可达
nc -zv localhost 9876

# 测试 Broker 是否可达
nc -zv localhost 10911
```

**解决方法**：
- 确保 Docker 容器已启动：`docker-compose ps`
- 等待服务完全启动（约 15 秒）
- 检查防火墙设置

### 问题 3：Consumer 无法消费消息

**检查步骤**：
1. 访问 Console（http://localhost:8081）
2. 查看 Topic 是否有消息
3. 查看消费者组状态
4. 检查应用日志

---

## 📊 性能调优

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

---

## 🗂️ 目录结构

```
task-async-service/
├── docker-compose.yml          # Docker Compose 配置
├── .env                        # 环境变量
├── docker-start.sh             # 启动脚本
├── docker-stop.sh              # 停止脚本
├── docker/
│   ├── rocketmq/
│   │   └── broker.conf        # Broker 配置
│   └── mysql/
│       └── init.sql           # MySQL 初始化脚本
└── src/main/resources/
    └── application-docker.yml  # Docker 环境配置
```

---

## 🔐 默认凭据

### MySQL

- **Host**: localhost:3306
- **Database**: task_db
- **Username**: task_user
- **Password**: task_pass
- **Root Password**: root123

连接示例：
```bash
mysql -h localhost -P 3306 -u task_user -ptask_pass task_db
```

---

## 📚 相关文档

- **项目 README**: `README.md`
- **实验记录**: `../README.md`
- **RocketMQ 官方文档**: https://rocketmq.apache.org/

---

## 💡 最佳实践

1. **开发时**：使用 Docker Compose 启动依赖，应用在本地运行
2. **测试时**：所有服务（包括应用）都运行在 Docker 中
3. **清理**：定期使用 `docker-compose down -v` 清理数据
4. **日志**：使用 `docker-compose logs -f` 实时查看日志

---

## 🎓 学习要点

通过这个 Docker 环境，你学到了：

1. **Docker Compose 基础**
   - 定义多服务应用
   - 服务依赖管理（`depends_on`）
   - 健康检查（`healthcheck`）

2. **服务编排**
   - RocketMQ NameServer + Broker 配置
   - 网络隔离（`networks`）
   - 数据持久化（`volumes`）

3. **环境隔离**
   - 开发环境 vs Docker 环境
   - Spring Profile 配置
   - 环境变量管理

---

## ✅ 检查清单

启动前检查：
- [ ] Docker 已安装并运行
- [ ] 端口 9876, 10911, 8081, 3306 未被占用
- [ ] JDK 17+ 已安装
- [ ] Maven 已安装

启动后验证：
- [ ] `docker-compose ps` 显示所有容器运行
- [ ] 可访问 http://localhost:8081
- [ ] 应用能连接到 RocketMQ
- [ ] 能提交和查询任务

---

**准备好了吗？运行 `./docker-start.sh` 开始吧！** 🚀
