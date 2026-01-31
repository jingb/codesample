#!/bin/bash

# Real-time RocketMQ Consumption Monitor
# Monitors message consumption rate in real-time

set -e

BROKER_CONTAINER="rocketmq-broker"
NAMESRV_ADDR="namesrv:9876"
CONSUMER_GROUP="task-consumer-group"
INTERVAL=5  # Check every 5 seconds

echo "=========================================="
echo "RocketMQ 实时消费速率监控"
echo "=========================================="
echo "监控间隔: ${INTERVAL} 秒"
echo "按 Ctrl+C 停止"
echo ""

# Record previous offset
PREV_OFFSET=0
PREV_TIME=$(date +%s)

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - PREV_TIME))

    # Get consumer progress
    OUTPUT=$(docker exec $BROKER_CONTAINER sh -c "cd /home/rocketmq/rocketmq-5.3.0 && ./bin/mqadmin consumerProgress -n $NAMESRV_ADDR -g $CONSUMER_GROUP" 2>&1)

    # Extract total consumed
    CURRENT_OFFSET=$(echo "$OUTPUT" | grep "task-topic" | awk '{sum+=$5} END {print sum}')
    TOTAL_DIFF=$(echo "$OUTPUT" | grep "Consume Diff Total" | awk '{print $4}')

    # Calculate rate
    if [ "$PREV_OFFSET" != "0" ] && [ "$ELAPSED" -gt "0" ]; then
        MSG_CONSUMED=$((CURRENT_OFFSET - PREV_OFFSET))
        RATE=$(echo "scale=2; $MSG_CONSUMED / $ELAPSED" | bc)
        echo "[$(date '+%H:%M:%S')] 已消费: $CURRENT_OFFSET | 本次新增: $MSG_CONSUMED | 消费速率: $RATE 消息/秒 | 堆积: $TOTAL_DIFF"
    else
        echo "[$(date '+%H:%M:%S')] 已消费: $CURRENT_OFFSET | 堆积: $TOTAL_DIFF"
    fi

    # Update previous values
    PREV_OFFSET=$CURRENT_OFFSET
    PREV_TIME=$CURRENT_TIME

    sleep $INTERVAL
done
