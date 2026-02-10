#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PID_DIR=/tmp
PROVIDER_PID_FILE="$PID_DIR/aoaojiao-rpc-provider-dev.pid"
CONSUMER_PID_FILE="$PID_DIR/aoaojiao-rpc-consumer-dev.pid"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn not found. Please install Maven."
  exit 1
fi

cd "$ROOT_DIR"

echo "[1/2] Start provider (spring-boot:run, requires Nacos at 127.0.0.1:8848)"
nohup mvn -pl rpc-example/example-boot-provider -DskipTests spring-boot:run > /tmp/aoaojiao-rpc-provider-dev.log 2>&1 &
PROVIDER_PID=$!

echo "$PROVIDER_PID" > "$PROVIDER_PID_FILE"

sleep 2

echo "[2/2] Start consumer (spring-boot:run)"
nohup mvn -pl rpc-example/example-boot-consumer -DskipTests spring-boot:run > /tmp/aoaojiao-rpc-consumer-dev.log 2>&1 &
CONSUMER_PID=$!

echo "$CONSUMER_PID" > "$CONSUMER_PID_FILE"

echo "Provider pid=$PROVIDER_PID"
echo "Consumer pid=$CONSUMER_PID"
echo "Logs: /tmp/aoaojiao-rpc-provider-dev.log /tmp/aoaojiao-rpc-consumer-dev.log"
