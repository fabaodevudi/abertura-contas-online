# Instruções de Instalação

## Ordem de Instalação

Os projetos devem ser instalados nesta ordem:

### 1. shared-kafka (PRIMEIRO)
```bash
cd shared-kafka
mvn clean install
```

Este projeto deve ser instalado primeiro pois os outros dependem dele.

### 2. aco-service
```bash
cd aco-service
mvn clean install
mvn spring-boot:run
```

### 3. notification-service
```bash
cd notification-service
mvn clean install
mvn spring-boot:run
```

## Estrutura

Cada projeto é independente e pode ser executado separadamente:

- **shared-kafka**: Biblioteca compartilhada (não é executável)
- **aco-service**: Serviço principal na porta 8080
- **notification-service**: Serviço de notificações na porta 8081

## Dependências

- **aco-service** depende de **shared-kafka**
- **notification-service** depende de **shared-kafka**

Ambos os serviços podem rodar independentemente após instalar shared-kafka.
