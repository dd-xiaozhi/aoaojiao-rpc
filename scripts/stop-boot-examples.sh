#!/usr/bin/env bash
set -euo pipefail

PID_DIR=/tmp
PROVIDER_PID_FILE="$PID_DIR/aoaojiao-rpc-provider.pid"
CONSUMER_PID_FILE="$PID_DIR/aoaojiao-rpc-consumer.pid"

kill_if_running() {
  local pid_file="$1"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if ps -p "$pid" > /dev/null 2>&1; then
      kill "$pid"
      echo "Stopped process $pid"
    fi
    rm -f "$pid_file"
  fi
}

kill_if_running "$CONSUMER_PID_FILE"
kill_if_running "$PROVIDER_PID_FILE"
