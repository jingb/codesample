#!/bin/bash

# Log Analysis Script for Stress Test Results
# Analyzes Docker logs to extract task execution metrics

set -e

if [ $# -lt 1 ]; then
    echo "Usage: $0 <submission_log_file> [docker_logs_file]"
    echo ""
    echo "Example:"
    echo "  $0 ./stress-test-results/submissions_20260125_060000.txt"
    echo "  $0 ./stress-test-results/submissions_20260125_060000.txt task-app.log"
    echo ""
    exit 1
fi

SUBMISSION_LOG="$1"
DOCKER_LOGS="${2:-}"

OUTPUT_DIR=$(dirname "$SUBMISSION_LOG")
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
ANALYSIS_FILE="$OUTPUT_DIR/analysis_$TIMESTAMP.txt"

echo "=========================================="
echo "Stress Test Log Analysis"
echo "=========================================="
echo "Submission Log: $SUBMISSION_LOG"
echo ""

# Extract submission times
echo "Extracting task submission times..."
SUBMISSION_COUNT=$(tail -n +2 "$SUBMISSION_LOG" | wc -l | tr -d ' ')
echo "Total submissions: $SUBMISSION_COUNT"
echo ""

# If docker logs file not provided, fetch from container
if [ -z "$DOCKER_LOGS" ]; then
    echo "Fetching logs from Docker container..."
    DOCKER_LOGS="$OUTPUT_DIR/docker_logs_$TIMESTAMP.txt"
    docker logs task-app > "$DOCKER_LOGS" 2>&1
    echo "Docker logs saved to: $DOCKER_LOGS"
    echo ""
fi

# Analyze logs
echo "Analyzing execution logs..."
echo ""

# Extract task start times with activeTasks count
echo "1. Task Start Events (with concurrency):"
echo "------------------------------------------"
grep "Task started:" "$DOCKER_LOGS" | tail -20

echo ""
echo "2. Maximum Concurrency Observed:"
echo "------------------------------------------"
MAX_CONCURRENCY=$(grep "Task started:" "$DOCKER_LOGS" | grep -oP 'activeTasks=\K\d+' | sort -n | tail -1)
echo "Max active tasks: $MAX_CONCURRENCY"

echo ""
echo "3. Task Completion Events:"
echo "------------------------------------------"
COMPLETED_COUNT=$(grep "Task executed successfully" "$DOCKER_LOGS" | wc -l | tr -d ' ')
echo "Tasks completed: $COMPLETED_COUNT / $SUBMISSION_COUNT"

echo ""
echo "4. Execution Duration Statistics:"
echo "------------------------------------------"
# Extract first task start time
FIRST_START=$(grep "Task started:" "$DOCKER_LOGS" | head -1 | grep -oP '\d{2}:\d{2}:\d{2}' | head -1)
# Extract last task completion time
LAST_END=$(grep "Task executed successfully" "$DOCKER_LOGS" | tail -1 | grep -oP '\d{2}:\d{2}:\d{2}' | head -1)

echo "First task started at: $FIRST_START"
echo "Last task completed at: $LAST_END"

echo ""
echo "5. Concurrency Distribution:"
echo "------------------------------------------"
echo "ActiveTasks Count | Frequency"
grep "Task started:" "$DOCKER_LOGS" | grep -oP 'activeTasks=\K\d+' | sort -n | uniq -c | awk '{print "         " $2 " | " $1}'

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Total Tasks Submitted: $SUBMISSION_COUNT"
echo "Tasks Completed: $COMPLETED_COUNT"
echo "Max Concurrency: $MAX_CONCURRENCY"
echo ""
echo "Analysis saved to: $ANALYSIS_FILE"
echo ""

# Save full analysis to file
{
    echo "Stress Test Analysis Report"
    echo "Generated: $(date)"
    echo ""
    echo "Configuration:"
    echo "  Submission Log: $SUBMISSION_LOG"
    echo "  Docker Logs: $DOCKER_LOGS"
    echo ""
    echo "Results:"
    echo "  Total Tasks: $SUBMISSION_COUNT"
    echo "  Completed: $COMPLETED_COUNT"
    echo "  Success Rate: $(echo "scale=2; $COMPLETED_COUNT * 100 / $SUBMISSION_COUNT" | bc)%"
    echo "  Max Concurrency: $MAX_CONCURRENCY"
    echo ""
    echo "Concurrency Distribution:"
    grep "Task started:" "$DOCKER_LOGS" | grep -oP 'activeTasks=\K\d+' | sort -n | uniq -c | awk '{print "  " $2 " active tasks: " $1 " times"}'
} > "$ANALYSIS_FILE"

echo "Full report saved to: $ANALYSIS_FILE"
