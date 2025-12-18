#!/bin/bash

echo "=== Iniciando serviços Docker ==="

docker network create abertura-conta-network 2>/dev/null || echo "Rede já existe"

echo "Iniciando Zookeeper..."
docker run -d --name zookeeper --network abertura-conta-network \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:7.5.0

sleep 5

echo "Iniciando Kafka..."
docker run -d --name kafka --network abertura-conta-network \
  --link zookeeper:zookeeper \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  confluentinc/cp-kafka:7.5.0

sleep 10

echo "Construindo imagens..."
docker build -f aco-service/Dockerfile -t aco-service:latest .
docker build -f notification-service/Dockerfile -t notification-service:latest .

echo "Iniciando ACO Service..."
docker run -d --name aco-service --network abertura-conta-network \
  --link kafka:kafka \
  -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  aco-service:latest

sleep 5

echo "Iniciando Notification Service..."
docker run -d --name notification-service --network abertura-conta-network \
  --link kafka:kafka \
  -p 8081:8081 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SERVER_PORT=8081 \
  notification-service:latest

sleep 10

echo ""
echo "=== Serviços iniciados ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "✅ ACO Service: http://localhost:8080"
echo "✅ Notification Service: http://localhost:8081"
echo "✅ Kafka: localhost:9092"
echo "✅ Zookeeper: localhost:2181"

