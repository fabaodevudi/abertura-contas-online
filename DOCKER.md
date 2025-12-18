# Docker - Guia de Execução

## 🐳 Serviços Docker

O projeto está configurado para rodar completamente no Docker.

### Estrutura

- **zookeeper**: Gerenciamento de cluster Kafka
- **kafka**: Message broker para comunicação assíncrona
- **aco-service**: Serviço principal (porta 8080)
- **notification-service**: Serviço de notificações (porta 8081)

## 🚀 Iniciar Serviços

### Opção 1: Script Automático

```bash
./docker-start.sh
```

### Opção 2: Manual

```bash
# 1. Criar rede
docker network create abertura-conta-network

# 2. Iniciar Zookeeper
docker run -d --name zookeeper --network abertura-conta-network \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  confluentinc/cp-zookeeper:7.5.0

# 3. Iniciar Kafka
docker run -d --name kafka --network abertura-conta-network \
  -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:9093 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  confluentinc/cp-kafka:7.5.0

# 4. Construir imagens
docker build -f aco-service/Dockerfile -t aco-service:latest .
docker build -f notification-service/Dockerfile -t notification-service:latest .

# 5. Iniciar ACO Service
docker run -d --name aco-service --network abertura-conta-network \
  -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  aco-service:latest

# 6. Iniciar Notification Service
docker run -d --name notification-service --network abertura-conta-network \
  -p 8081:8081 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SERVER_PORT=8081 \
  notification-service:latest
```

## 🛑 Parar Serviços

```bash
./docker-stop.sh
```

Ou manualmente:

```bash
docker stop notification-service aco-service kafka zookeeper
docker rm notification-service aco-service kafka zookeeper
```

## ✅ Verificar Status

```bash
# Listar containers
docker ps

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Logs
docker logs aco-service
docker logs notification-service
docker logs kafka
```

## 📍 Endpoints

- **ACO Service**: http://localhost:8080
  - API: http://localhost:8080/api/solicitacoes
  - Swagger: http://localhost:8080/swagger-ui.html
  - Camunda: http://localhost:8080/camunda/app/cockpit/default/
  - Health: http://localhost:8080/actuator/health

- **Notification Service**: http://localhost:8081
  - Health: http://localhost:8081/actuator/health

- **Kafka**: localhost:9092
- **Zookeeper**: localhost:2181

## 🧪 Testar API

```bash
curl -X POST http://localhost:8080/api/solicitacoes \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: meu-test-id" \
  -d '{
    "cpf": "12345678901",
    "nome": "João Silva",
    "email": "joao@email.com",
    "telefone": "11987654321",
    "canal": "AMERICA"
  }'
```

## 🔍 Observabilidade

O Correlation ID é propagado automaticamente:
- Headers HTTP: `X-Correlation-Id`
- Logs: Incluem `[correlationId]` no formato
- Kafka: Headers das mensagens incluem Correlation ID

Verificar nos logs:
```bash
docker logs aco-service | grep correlationId
```

