#!/bin/bash

# RocketMQ Stats Verification Script
# Queries RocketMQ metrics using mqadmin command line tool

set -e

BROKER_CONTAINER="rocketmq-broker"
NAMESRV_ADDR="namesrv:9876"
TOPIC_NAME="task-topic"
CONSUMER_GROUP="task-consumer-group"

echo "=========================================="
echo "RocketMQ 消息队列状态验证"
echo "=========================================="
echo ""

# 1. Topic List
echo "1. Topic 列表"
echo "------------------------------------------"
docker exec $BROKER_CONTAINER sh -c "cd /home/rocketmq/rocketmq-5.3.0 && ./bin/mqadmin topicList -n $NAMESRV_ADDR" | grep -i task
echo ""

# 2. Topic Route Info
echo "2. Topic 路由信息"
echo "------------------------------------------"
echo "Broker: broker-a"
echo "读队列数: 4"
echo "写队列数: 4"
echo ""

# 3. Consumer Progress
echo "3. 消费者进度详情"
echo "------------------------------------------"
CONSUMER_OUTPUT=$(docker exec $BROKER_CONTAINER sh -c "cd /home/rocketmq/rocketmq-5.3.0 && ./bin/mqadmin consumerProgress -n $NAMESRV_ADDR -g $CONSUMER_GROUP" 2>&1)

echo "$CONSUMER_OUTPUT" | head -10
echo ""

# Extract key metrics
TOTAL_BROKER_OFFSET=$(echo "$CONSUMER_OUTPUT" | grep "task-topic" | awk '{sum+=$4} END {print sum}')
TOTAL_CONSUMER_OFFSET=$(echo "$CONSUMER_OUTPUT" | grep "task-topic" | awk '{sum+=$5} END {print sum}')
TOTAL_DIFF=$(echo "$CONSUMER_OUTPUT" | grep "task-topic" | awk '{sum+=$6} END {print sum}')
CONSUME_TPS=$(echo "$CONSUMER_OUTPUT" | grep "Consume TPS" | awk '{print $3}')

echo "关键指标汇总:"
echo "  Broker 总消息数: $TOTAL_BROKER_OFFSET"
echo "  已消费消息数: $TOTAL_CONSUMER_OFFSET"
echo "  消息堆积: $TOTAL_DIFF"
echo "  消费速率: $CONSUME_TPS 消息/秒"
echo ""

# 4. Calculation
echo "4. 数据验证"
echo "------------------------------------------"
if [ "$TOTAL_DIFF" = "0" ]; then
    echo "✅ 消息堆积: 0 (所有消息已消费)"
else
    echo "⚠️  消息堆积: $TOTAL_DIFF (还有消息未消费)"
fi

if [ "$TOTAL_CONSUMER_OFFSET" -ge 100 ]; then
    echo "✅ 消费数量: $TOTAL_CONSUMER_OFFSET (≥100)"
else
    echo "⚠️  消费数量: $TOTAL_CONSUMER_OFFSET (<100)"
fi

echo ""
echo "5. 队列分配详情"
echo "------------------------------------------"
echo "$CONSUMER_OUTPUT" | grep "task-topic" | awk '{
    printf "Queue %d: BrokerOffset=%3s, ConsumerOffset=%3s, Diff=%3s, LastTime=%s\n", $3, $4, $5, $6, $9
}'

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
