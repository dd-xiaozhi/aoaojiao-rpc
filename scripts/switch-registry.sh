#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 [local|nacos]"
  exit 1
fi

MODE="$1"

set_prop() {
  local file="$1"
  local key="$2"
  local value="$3"
  if grep -q "^${key}=" "$file"; then
    sed -i.bak "s#^${key}=.*#${key}=${value}#" "$file" && rm -f "${file}.bak"
  else
    echo "${key}=${value}" >> "$file"
  fi
}

update_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    return
  fi
  if [ "$MODE" = "local" ]; then
    set_prop "$file" "aoaojiao.rpc.registry.enabled" "false"
    set_prop "$file" "aoaojiao.rpc.registry.serverAddr" ""
    set_prop "$file" "aoaojiao.rpc.registry.mode" "local"
  else
    set_prop "$file" "aoaojiao.rpc.registry.enabled" "true"
    set_prop "$file" "aoaojiao.rpc.registry.serverAddr" "127.0.0.1:8848"
    set_prop "$file" "aoaojiao.rpc.registry.mode" "nacos"
  fi
}

update_file "rpc-example/example-boot-provider/src/main/resources/application.properties"
update_file "rpc-example/example-boot-consumer/src/main/resources/application.properties"

echo "Switched registry mode to: $MODE"
