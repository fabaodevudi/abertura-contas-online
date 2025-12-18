# Sistema de Abertura de Conta Online

Sistema de abertura de contas online para banco genérico, utilizando **Spring Boot 3.x** e **Camunda BPM** para orquestração de processos. Projeto desenvolvido com **Java 21** e seguindo padrão **DDD (Domain-Driven Design)**.

## ✨ Características

- ✅ **Java 21** com recursos modernos (Records, Text Blocks, Pattern Matching)
- ✅ **Spring Boot 3.2.0** com suporte a Jakarta EE
- ✅ **Camunda BPM 7.21.0** para orquestração de processos
- ✅ **Arquitetura DDD** com separação clara de responsabilidades
- ✅ **Arquitetura de Microserviços** com Kafka para comunicação assíncrona
- ✅ **Design Patterns** (Strategy + Factory + Facade) para notificações multi-canal
- ✅ **Java Delegates** para validações assíncronas
- ✅ **API REST** completa com validações
- ✅ **Notificações Multi-Canal** (Flamengo, Azul, América) com Email, SMS e Push

## 🚀 Início Rápido

### Pré-requisitos

- **Java 21** ou superior
- **Maven 3.6+**
- **Kafka** rodando em `localhost:9092` (ou configurar no `application.yml`)

### Executar

#### Opção 1: Executar todos os serviços

```bash
# Compilar projeto
mvn clean install

# Terminal 1: ACO Service
cd aco-service
mvn spring-boot:run

# Terminal 2: Notification Service
cd notification-service
mvn spring-boot:run
```

#### Opção 2: Executar apenas ACO Service (sem notificações)

```bash
mvn spring-boot:run
```

### Acessar

- **ACO Service - API REST:** http://localhost:8080/api/solicitacoes
- **ACO Service - Camunda Cockpit:** http://localhost:8080/camunda/app/cockpit/default/
  - Usuário: `admin`
  - Senha: `admin`
- **ACO Service - H2 Console:** http://localhost:8080/h2-console
- **Notification Service:** http://localhost:8081 (health check)

## 📚 Documentação Completa

Consulte os arquivos de documentação:

- **[DOCUMENTACAO.md](./DOCUMENTACAO.md)** - Documentação completa do sistema
  - Arquitetura detalhada
  - Fluxos BPMN em ASCII
  - Endpoints da API
  - Java Delegates
  - Instruções de monitoramento no Camunda
  - Exemplos de uso

- **[README_MICROSERVICOS.md](./README_MICROSERVICOS.md)** - Arquitetura de Microserviços
  - Estrutura de módulos
  - Design Patterns implementados
  - Fluxo de eventos com Kafka
  - Como executar os serviços

- **[NARRATIVA_KAFKA_MICROSERVICOS.md](./NARRATIVA_KAFKA_MICROSERVICOS.md)** - Narrativa técnica sobre evolução para microserviços
  - Contexto e desafios
  - Solução implementada
  - Design Patterns (Strategy + Factory + Facade)
  - Pontos para discussão em entrevista

- **[FLUXO_ASSINCRONO.md](./FLUXO_ASSINCRONO.md)** - Fluxo assíncrono (Padrão ACO)
  - Como funciona o padrão de eventos assíncronos
  - Diagramas de sequência
  - Código de referência
  - Troubleshooting

- **[GUIA_MONITORAMENTO.md](./GUIA_MONITORAMENTO.md)** - Guia de monitoramento no Camunda Cockpit

- **[COMO_VER_INSTANCIAS.md](./COMO_VER_INSTANCIAS.md)** - Como visualizar instâncias de processo
- **[COMO_ADICIONAR_FILTROS.md](./COMO_ADICIONAR_FILTROS.md)** - Guia passo a passo para adicionar filtros no Cockpit
- **[NARRATIVA_TECNICA_ENTREVISTA.md](./NARRATIVA_TECNICA_ENTREVISTA.md)** - Narrativa técnica para entrevistas sobre a implementação

## 🏗️ Estrutura do Projeto (Microserviços + DDD)

O projeto foi evoluído para uma arquitetura de microserviços:

```
abertura-conta-online/
├── shared-kafka/              # Módulo compartilhado
│   └── src/main/java/br/com/banco/shared/kafka/
│       ├── config/           # KafkaTopics
│       ├── events/           # ContaAbertaEvent, SolicitacaoRejeitadaEvent
│       └── util/             # CorrelationIdUtil
│
├── aco-service/              # Serviço principal (Abertura de Conta)
│   └── src/main/java/br/com/banco/aberturaconta/
│       ├── core/            # Camada de Domínio (DDD)
│       │   ├── domain/       # StatusSolicitacao
│       │   ├── model/        # SolicitacaoAberturaConta
│       │   └── service/      # ISolicitacaoService
│       ├── infra/            # Camada de Infraestrutura
│       │   ├── bpmn/         # Java Delegates (Camunda)
│       │   ├── kafka/        # Kafka Producer
│       │   └── rest/         # Controllers REST
│       └── resources/
│           └── processes/   # Processos BPMN
│
└── notification-service/     # Microserviço de Notificações
    └── src/main/java/br/com/banco/notification/
        ├── application/
        │   ├── notificacao/  # Strategy + Factory + Facade
        │   └── SolicitacaoStatusFinalConsumer.java
        └── core/service/     # EmailService, SmsService, PushService
```

### Estrutura DDD (dentro de cada serviço)

```
core/                    # Camada de Domínio (DDD)
├── domain/             # Entidades de domínio
├── model/              # Modelos de domínio
└── service/            # Interfaces e implementações

infra/                  # Camada de Infraestrutura
├── bpmn/               # Java Delegates (Camunda)
├── kafka/              # Kafka Producer/Consumer
├── dto/                # DTOs (Records Java 21)
├── repository/         # Repositórios JPA
└── rest/               # Controllers REST
```

## 🔄 Fluxo de Validações

1. **Topaz** - Validação de dispositivo e score de segurança
2. **Antifraude** - Validação antifraude
3. **PIX** - Consulta de fraudes PIX
4. **Serasa** - Consulta de score Serasa
5. **Prova de Vida** - Validação biométrica
6. **Sistema Interno** - Abertura de conta

## ⚡ Fluxo Assíncrono (Padrão ACO + Kafka)

O sistema implementa um **fluxo assíncrono** seguindo o padrão do ACO, evoluído para microserviços com Kafka:

1. **Requisição HTTP** → Controller recebe e valida
2. **Salva Solicitação** → Service salva no banco (transação)
3. **Publica Evento** → Evento publicado dentro da transação
4. **Retorna HTTP 201** → Resposta imediata (~0.7s) ✅
5. **Após Commit** → Listener assíncrono inicia processo Camunda
6. **Processo Executa** → Validações sequenciais em background
7. **Status Final** → Camunda publica evento no Kafka (CONTA_ABERTA ou REJEITADA)
8. **Notification Service** → Consome evento e envia notificações (Email, SMS, Push)

**Vantagens:**
- ✅ Resposta HTTP imediata (não bloqueia)
- ✅ Processo inicia apenas após commit (garantia de persistência)
- ✅ Execução em thread separada (não bloqueia requisições)
- ✅ Comunicação assíncrona via Kafka (desacoplamento)
- ✅ Notificações em microserviço separado (escalabilidade independente)
- ✅ Padrão consistente com ACO

Veja a seção [Fluxo Assíncrono](#-fluxo-assíncrono-padrão-aco) na documentação completa para detalhes.

## 📝 Exemplo de Uso

### Criar Solicitação

```bash
curl -X POST http://localhost:8080/api/solicitacoes \
  -H "Content-Type: application/json" \
  -d '{
    "cpf": "12345678901",
    "nome": "João Silva",
    "email": "joao.silva@email.com",
    "telefone": "11987654321"
  }'
```

### Consultar Solicitação

```bash
curl http://localhost:8080/api/solicitacoes/1
```

## 📊 Monitoramento

Acesse o **Camunda Cockpit** para monitorar:

- Instâncias de processo em execução
- Histórico de processos
- Variáveis do processo
- Erros e incidentes
- Métricas de performance

**URL:** http://localhost:8080/camunda/app/cockpit/default/

## 🎓 Tecnologias

### Core
- **Java 21** - LTS com recursos modernos
- **Spring Boot 3.2.0** - Framework principal
- **Camunda BPM 7.21.0** - Orquestração de processos

### Microserviços e Comunicação
- **Apache Kafka** - Message broker para comunicação assíncrona
- **Spring Kafka** - Integração Spring com Kafka
- **Event-Driven Architecture** - Arquitetura baseada em eventos

### Persistência
- **H2 Database** - Banco em memória (desenvolvimento)
- **JPA/Hibernate** - ORM
- **Jakarta Persistence** - API de persistência

### Design Patterns
- **Strategy Pattern** - Notificações por canal (Flamengo, Azul, América)
- **Factory Pattern** - Seleção de strategies
- **Facade Pattern** - Interface única para notificações

### Outras
- **Lombok** - Redução de boilerplate
- **SpringDoc OpenAPI 2.3.0** - Documentação da API
- **Jakarta Validation** - Validação de dados

## 🆕 Recursos do Java 21 Utilizados

### Records
DTOs implementados como **Records** para imutabilidade e código mais conciso:
```java
public record SolicitacaoAberturaContaDTO(
    @NotBlank String cpf,
    @NotBlank String nome,
    @Email String email,
    @Pattern(regexp = "\\d{10,11}") String telefone
) {}
```

### Text Blocks
Uso de **Text Blocks** para strings multilinha:
```java
log.info("""
    === LOG DE PROCESSO ===
    Solicitação: {}
    Etapa: {}
    ======================
    """, solicitacaoId, etapa);
```

### Type Inference (var)
Uso extensivo de `var` para código mais limpo:
```java
var solicitacaoId = Long.parseLong(execution.getBusinessKey());
var aprovado = validarTopaz(solicitacaoId);
```

### Melhorias Gerais
- ✅ Migração completa para **Jakarta EE** (javax → jakarta)
- ✅ Remoção de `throws Exception` desnecessários
- ✅ Tratamento de exceções melhorado
- ✅ Código mais funcional e expressivo

## 🔧 Compilação e Execução

### Compilar

```bash
mvn clean install
```

### Executar

```bash
mvn spring-boot:run
```

### Executar JAR

```bash
java -jar target/abertura-conta-online-1.0.0-SNAPSHOT.jar
```

## 📖 Documentação

Para documentação completa com:
- Arquitetura detalhada
- Fluxos BPMN em ASCII
- Endpoints da API
- Java Delegates
- Instruções de monitoramento no Camunda
- Exemplos de uso

Consulte [DOCUMENTACAO.md](./DOCUMENTACAO.md).

## 🎯 Melhorias do Java 21

Este projeto aproveita os recursos modernos do Java 21:

- **Records** para DTOs imutáveis
- **Text Blocks** para strings multilinha
- **Type Inference (var)** para código mais limpo
- **Jakarta EE** em vez de Java EE
- **Tratamento de exceções** melhorado
- **Código mais funcional** e expressivo

## 📝 Licença

Este é um projeto de demonstração e estudo.

