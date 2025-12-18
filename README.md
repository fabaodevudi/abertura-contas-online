# Abertura de Conta Online - Projetos Independentes

Este repositório contém três projetos Maven independentes:

## Projetos

### 1. shared-kafka
Biblioteca compartilhada com eventos Kafka, Correlation ID e configurações.

**Instalação local:**
```bash
cd shared-kafka
mvn clean install
```

### 2. aco-service
Serviço principal de abertura de contas online com Camunda BPM.

**Pré-requisito:** Instalar `shared-kafka` primeiro.

**Execução:**
```bash
cd aco-service
mvn clean install
mvn spring-boot:run
```

### 3. notification-service
Microserviço de notificações (email, SMS, push) com suporte multi-canal.

**Pré-requisito:** Instalar `shared-kafka` primeiro.

**Execução:**
```bash
cd notification-service
mvn clean install
mvn spring-boot:run
```

## Ordem de Instalação

1. `shared-kafka` (deve ser instalado primeiro)
2. `aco-service` (depende de shared-kafka)
3. `notification-service` (depende de shared-kafka)

## Documentação

Consulte a pasta `documentos/` para documentação completa.
