#!/bin/bash

# Stress Test Script for Constant Throughput Control Verification
# This script submits a batch of tasks to the async service and records timing

set -e

# Configuration
TOTAL_TASKS=100
TASK_TYPE="DATA_EXPORT"
API_URL="http://localhost:8080/tasks"
OUTPUT_DIR="./stress-test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$OUTPUT_DIR/stress_test_$TIMESTAMP.txt"
SUBMISSION_LOG="$OUTPUT_DIR/submissions_$TIMESTAMP.txt"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "=========================================="
echo "Stress Test: Constant Throughput Control"
echo "=========================================="
echo "Total Tasks: $TOTAL_TASKS"
echo "Task Type: $TASK_TYPE"
echo "API URL: $API_URL"
echo "Results will be saved to: $RESULTS_FILE"
echo ""
echo "Starting task submission..."
echo ""

# Record start time
START_TIME=$(date +%s)

# Submit tasks and record submission times
echo "taskId,submissionTime,status" > "$SUBMISSION_LOG"

for i in $(seq 1 $TOTAL_TASKS); do
    # Submit task
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{\"taskType\":\"$TASK_TYPE\",\"params\":{\"userId\":\"test-user-$i\"}}")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    # Extract taskId from response
    TASK_ID=$(echo "$BODY" | grep -o '"taskId":"[^"]*' | cut -d'"' -f4)

    CURRENT_TIME=$(date +"%Y-%m-%d %H:%M:%S.%3N")
    echo "$TASK_ID,$CURRENT_TIME,HTTP_$HTTP_CODE" >> "$SUBMISSION_LOG"

    # Progress indicator
    if [ $((i % 10)) -eq 0 ]; then
        echo "Submitted $i/$TOTAL_TASKS tasks..."
    fi
done

SUBMISSION_END_TIME=$(date +%s)
SUBMISSION_DURATION=$((SUBMISSION_END_TIME - START_TIME))

echo ""
echo "All tasks submitted in ${SUBMISSION_DURATION}s"
echo ""
echo "=========================================="
echo "Submission Summary"
echo "=========================================="
echo "Total Tasks Submitted: $TOTAL_TASKS"
echo "Time Taken: ${SUBMISSION_DURATION}s"
echo "Submission Rate: $(echo "scale=2; $TOTAL_TASKS / $SUBMISSION_DURATION" | bc) tasks/sec"
echo ""
echo "Submission log saved to: $SUBMISSION_LOG"
echo ""
echo "Next Steps:"
echo "1. Monitor task processing in application logs: docker logs -f task-app"
echo "2. Check RocketMQ Console: http://localhost:8081"
echo "3. Use analyze-logs.sh to analyze results after all tasks complete"
echo ""
echo "To monitor active tasks in real-time:"
echo "  docker logs -f task-app | grep 'activeTasks='"
echo ""
