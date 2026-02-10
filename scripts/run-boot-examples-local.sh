#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PID_DIR=/tmp
PROVIDER_PID_FILE="$PID_DIR/aoaojiao-rpc-provider-local.pid"
CONSUMER_PID_FILE="$PID_DIR/aoaojiao-rpc-consumer-local.pid"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn not found. Please install Maven."
  exit 1
fi

echo "[1/3] Build boot examples"
cd "$ROOT_DIR"
mvn -pl rpc-example/example-boot-provider,rpc-example/example-boot-consumer -am -DskipTests package

PROVIDER_JAR=$(ls -1 rpc-example/example-boot-provider/target/example-boot-provider-*.jar | head -n 1)
CONSUMER_JAR=$(ls -1 rpc-example/example-boot-consumer/target/example-boot-consumer-*.jar | head -n 1)

if [ ! -f "$PROVIDER_JAR" ] || [ ! -f "$CONSUMER_JAR" ]; then
  echo "Boot jars not found. Build may have failed."
  exit 1
fi

echo "[2/3] Start provider (local registry)"
nohup java -jar "$PROVIDER_JAR" --spring.profiles.active=local > /tmp/aoaojiao-rpc-provider-local.log 2>&1 &
PROVIDER_PID=$!

echo "$PROVIDER_PID" > "$PROVIDER_PID_FILE"

sleep 2

echo "[3/3] Start consumer (local registry)"
nohup java -jar "$CONSUMER_JAR" --spring.profiles.active=local > /tmp/aoaojiao-rpc-consumer-local.log 2>&1 &
CONSUMER_PID=$!

echo "$CONSUMER_PID" > "$CONSUMER_PID_FILE"

echo "Provider pid=$PROVIDER_PID"
echo "Consumer pid=$CONSUMER_PID"
echo "Logs: /tmp/aoaojiao-rpc-provider-local.log /tmp/aoaojiao-rpc-consumer-local.log"
