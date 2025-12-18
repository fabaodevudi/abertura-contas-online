#!/bin/bash

echo "=== Parando serviços Docker ==="

docker stop notification-service aco-service kafka zookeeper 2>/dev/null
docker rm notification-service aco-service kafka zookeeper 2>/dev/null

echo "✅ Serviços parados e removidos"

