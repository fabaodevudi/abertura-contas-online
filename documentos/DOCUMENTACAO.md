# Documentação - Sistema de Abertura de Conta Online

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
   - [Padrão DDD](#padrão-ddd-domain-driven-design)
   - [Arquitetura de Microserviços](#arquitetura-de-microserviços)
   - [Orquestração Camunda](#orquestração-camunda)
   - [Fluxo Assíncrono (Padrão ACO)](#-fluxo-assíncrono-padrão-aco)
3. [Observabilidade e Rastreabilidade](#observabilidade-e-rastreabilidade)
   - [Correlation ID](#correlation-id)
   - [Logs Estruturados](#logs-estruturados)
4. [Princípios de Qualidade](#princípios-de-qualidade)
   - [SOLID](#solid)
   - [Imutabilidade](#imutabilidade)
   - [Clean Code](#clean-code)
   - [Testes](#testes)
5. [Fluxo de Processo BPMN](#fluxo-de-processo-bpmn)
6. [Endpoints da API](#endpoints-da-api)
7. [Java Delegates](#java-delegates)
8. [Monitoramento do Camunda](#monitoramento-do-camunda)
9. [Instruções de Execução](#instruções-de-execução)

> 📖 **Documentação Detalhada do Fluxo Assíncrono**: Consulte [FLUXO_ASSINCRONO.md](./FLUXO_ASSINCRONO.md) para uma explicação completa com diagramas, código e troubleshooting.

> 🏗️ **Arquitetura de Microserviços**: O sistema foi evoluído para microserviços com Kafka. Consulte [README_MICROSERVICOS.md](./README_MICROSERVICOS.md) e [NARRATIVA_KAFKA_MICROSERVICOS.md](./NARRATIVA_KAFKA_MICROSERVICOS.md) para detalhes sobre a evolução arquitetural.

---

## 🎯 Visão Geral

Sistema de abertura de contas online para banco genérico, utilizando **Spring Boot** e **Camunda BPM** para orquestração de processos. O sistema segue o padrão **DDD (Domain-Driven Design)** e implementa validações assíncronas através de **Java Delegates**.

### Tecnologias Utilizadas

- **Spring Boot 3.2.0**
- **Camunda BPM 7.21.0**
- **Java 21**
- **Apache Kafka** - Comunicação assíncrona entre microserviços
- **H2 Database** (para desenvolvimento)
- **Lombok**
- **JPA/Hibernate**
- **Logback** - Logs estruturados com Correlation ID

### Estrutura do Projeto (Microserviços + DDD)

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

---

## 🏗️ Arquitetura

### Padrão DDD (Domain-Driven Design)

O projeto segue o padrão DDD com separação clara de responsabilidades:

1. **Core (Domínio)**: Contém as regras de negócio e entidades
2. **Infra (Infraestrutura)**: Implementações técnicas (repositórios, controllers, delegates)

Cada microserviço segue essa estrutura DDD internamente.

### Arquitetura de Microserviços

O sistema foi evoluído de um monolito para uma arquitetura de microserviços, mantendo o Camunda para orquestração interna e utilizando Kafka para comunicação entre serviços.

#### Módulos

1. **shared-kafka**: Eventos e configurações compartilhadas
2. **aco-service**: Serviço principal de abertura de contas (porta 8080)
3. **notification-service**: Microserviço de notificações (porta 8081)

#### Fluxo com Kafka

```
┌─────────────────────────────────────────────────────────────┐
│                    ACO SERVICE                              │
│                                                             │
│  Controller → Service → Camunda → Kafka Producer           │
│                                                             │
│  Quando status final (CONTA_ABERTA ou REJEITADA):          │
│  → Publica evento no Kafka                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    KAFKA BROKER                            │
│                                                             │
│  Tópicos:                                                   │
│  - conta-aberta                                            │
│  - solicitacao-rejeitada                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              NOTIFICATION SERVICE                         │
│                                                             │
│  Kafka Consumer → Facade → Factory → Strategy             │
│                                                             │
│  Strategy Pattern:                                         │
│  - NotificacaoFlamengo                                     │
│  - NotificacaoAzul                                         │
│  - NotificacaoAmerica                                      │
│                                                             │
│  Envia: Email, SMS, Push                                   │
└─────────────────────────────────────────────────────────────┘
```

#### Design Patterns Implementados

1. **Strategy Pattern**: Cada canal (Flamengo, Azul, América) tem sua própria implementação de notificação. A interface `NotificacaoStrategy` define métodos específicos para cada tipo de notificação (Email, SMS, Push), seguindo o padrão do projeto Padroes.
2. **Factory Pattern**: Seleciona a Strategy baseada no canal através do método `getNotificadorPorCanal()`.
3. **Facade Pattern**: Interface única que abstrai a complexidade dos canais e tipos de notificação. A Facade chama diretamente os métodos da estratégia, eliminando a necessidade de verificações `instanceof`.
4. **AbstractNotificacao**: Classe base que implementa a lógica comum de notificação, enquanto as subclasses apenas definem os templates específicos de cada marca.

**Evolução:** A implementação foi refatorada para seguir o padrão Strategy do projeto Padroes, onde a interface define métodos específicos e a Facade chama diretamente esses métodos na estratégia.

Para mais detalhes, consulte [README_MICROSERVICOS.md](./README_MICROSERVICOS.md) e [NARRATIVA_KAFKA_MICROSERVICOS.md](./NARRATIVA_KAFKA_MICROSERVICOS.md).

### Camadas

```
┌─────────────────────────────────────┐
│      REST Controllers (Infra)        │
│  SolicitacaoAberturaContaController │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Services (Core)                  │
│  ISolicitacaoService                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Domain (Core)                    │
│  SolicitacaoAberturaConta            │
│  StatusSolicitacao                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Repository (Infra)               │
│  SolicitacaoRepository               │
└──────────────────────────────────────┘
```

### Orquestração Camunda

O Camunda atua como orquestrador de processos, executando validações sequenciais através de **Java Delegates**. O sistema implementa um **fluxo assíncrono** seguindo o padrão do ACO, garantindo que a resposta HTTP retorne imediatamente sem aguardar o início do processo Camunda.

```
Controller REST
      │
      ▼
Salva Solicitação (Transação)
      │
      ▼
Publica Evento (Dentro da Transação)
      │
      ▼
Retorna HTTP 201 (Imediato) ✅
      │
      │ (Assíncrono - Após Commit)
      ▼
┌─────────────────────────────────────┐
│   TransactionalEventListener        │
│   (AFTER_COMMIT)                    │
│   + @Async                          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Processo BPMN                     │
│   (Orquestração)                    │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Java Delegates             │   │
│   │  (Validações)                │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Services                   │   │
│   │  (Regras de Negócio)        │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 🔄 Fluxo Assíncrono (Padrão ACO)

O sistema implementa um padrão de **eventos assíncronos** para garantir que a resposta HTTP seja retornada imediatamente, sem bloquear a requisição enquanto o processo Camunda é iniciado.

#### Componentes do Fluxo Assíncrono

1. **Controller REST** (`SolicitacaoAberturaContaController`)
   - Recebe a requisição HTTP
   - Valida os dados
   - Chama o service para salvar

2. **Service** (`SolicitacaoServiceImpl`)
   - Salva a solicitação no banco (dentro de uma transação `@Transactional`)
   - Publica o evento `SolicitacaoCriadaEvent` **dentro da transação**
   - Retorna a solicitação salva

3. **Evento** (`SolicitacaoCriadaEvent`)
   - Carrega o ID da solicitação e variáveis para o processo Camunda
   - Publicado pelo `ApplicationEventPublisher`

4. **Listener Assíncrono** (`SolicitacaoCriadaListener`)
   - Escuta o evento **após o commit** da transação (`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`)
   - Executa em thread separada (`@Async`)
   - Inicia o processo Camunda de forma assíncrona

#### Fluxo Detalhado

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUXO ASSÍNCRONO COMPLETO                   │
└─────────────────────────────────────────────────────────────────┘

1. CLIENTE → POST /api/solicitacoes
   │
   ▼
2. Controller.solicitarAberturaConta()
   │
   ├─► Valida CPF (não existe conta)
   │
   └─► service.salvar(solicitacao)
       │
       ├─► [INÍCIO TRANSAÇÃO @Transactional]
       │
       ├─► repository.save(solicitacao)
       │
       ├─► eventPublisher.publishEvent(
       │       new SolicitacaoCriadaEvent(id, variaveis)
       │   )
       │   │
       │   └─► Evento registrado para processamento após commit
       │
       ├─► [COMMIT TRANSAÇÃO]
       │
       └─► return solicitacao
           │
           ▼
3. Controller retorna HTTP 201 ✅ (Resposta Imediata)
   │
   │ (Em paralelo - Thread Assíncrona)
   │
   ▼
4. SolicitacaoCriadaListener.onSolicitacaoCriadaEvent()
   │
   ├─► [Thread: camunda-async-X]
   │
   ├─► Verifica se processo já existe
   │
   └─► runtimeService.createProcessInstanceByKey()
       │
       ├─► Processo: ProcessoAberturaContaPF
       ├─► Business Key: ID da Solicitação
       └─► Variáveis: solicitacaoId, cpf, nome
           │
           ▼
5. Processo Camunda Iniciado
   │
   └─► Executa Java Delegates sequencialmente
       ├─► ValidarTopazDelegate (60s delay)
       ├─► ValidarAntifraudeDelegate (60s delay)
       ├─► ValidarPixDelegate (60s delay)
       ├─► ValidarSerasaDelegate (60s delay)
       ├─► ValidarProvaVidaDelegate (60s delay)
       └─► AbrirContaSistemaInternoDelegate
```

#### Vantagens do Padrão Assíncrono

✅ **Resposta HTTP Imediata**: A API retorna em ~0.7 segundos, sem aguardar o processo Camunda

✅ **Garantia de Persistência**: O processo só inicia após o commit da transação, garantindo que a solicitação esteja salva

✅ **Não Bloqueia Threads**: O processamento do Camunda ocorre em thread separada, não bloqueando requisições HTTP

✅ **Resiliência**: Se houver erro ao iniciar o processo, a solicitação já está salva e pode ser reprocessada

✅ **Padrão ACO**: Segue o mesmo padrão usado no projeto ACO, garantindo consistência arquitetural

#### Código de Referência

**Evento:**
```java
public class SolicitacaoCriadaEvent {
    private final Long solicitacaoId;
    private final Map<String, Object> variaveis;
}
```

**Listener:**
```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onSolicitacaoCriadaEvent(SolicitacaoCriadaEvent event) {
    // Inicia processo Camunda de forma assíncrona
}
```

**Service:**
```java
@Transactional
public SolicitacaoAberturaConta salvar(SolicitacaoAberturaConta solicitacao) {
    SolicitacaoAberturaConta saved = repository.save(solicitacao);
    eventPublisher.publishEvent(new SolicitacaoCriadaEvent(saved.getId(), variaveis));
    return saved;
}
```

---

## 🔍 Observabilidade e Rastreabilidade

### Correlation ID

O sistema implementa **Correlation ID** para rastrear requisições através de múltiplos serviços e logs, permitindo correlacionar eventos relacionados à mesma operação.

#### Como Funciona

1. **HTTP Request**: O `CorrelationIdFilter` captura ou gera um Correlation ID no header `X-Correlation-Id`
2. **ThreadLocal + MDC**: O Correlation ID é armazenado no ThreadLocal e no MDC (Mapped Diagnostic Context) para logs
3. **Propagação**: O Correlation ID é propagado automaticamente em:
   - **Logs**: Todos os logs incluem o Correlation ID via MDC
   - **Kafka**: Headers das mensagens Kafka incluem o Correlation ID
   - **HTTP Response**: Header `X-Correlation-Id` é retornado na resposta

#### Componentes Implementados

**1. CorrelationIdUtil** (`shared-kafka/util/CorrelationIdUtil.java`)
```java
public class CorrelationIdUtil {
    private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();
    
    public static String getCorrelationId() {
        String correlationId = correlationIdHolder.get();
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }
    
    public static void setCorrelationId(String correlationId) {
        correlationIdHolder.set(correlationId);
        MDC.put("correlationId", correlationId); // Para logs
    }
}
```

**2. CorrelationIdFilter** (`infra/config/CorrelationIdFilter.java`)
```java
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = CorrelationIdUtil.getCorrelationId();
        } else {
            CorrelationIdUtil.setCorrelationId(correlationId);
        }
        response.setHeader("X-Correlation-Id", correlationId);
        filterChain.doFilter(request, response);
    }
}
```

**3. KafkaCorrelationIdInterceptor** (`infra/config/KafkaCorrelationIdInterceptor.java`)
```java
public class KafkaCorrelationIdInterceptor implements ProducerInterceptor<String, String> {
    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        record.headers().add("X-Correlation-Id", correlationId.getBytes());
        return record;
    }
}
```

#### Fluxo de Propagação

```
Cliente HTTP
    │
    │ X-Correlation-Id: abc-123 (opcional)
    ▼
┌─────────────────────────────────────┐
│ CorrelationIdFilter                 │
│ ├─ Captura ou gera Correlation ID   │
│ ├─ Armazena em ThreadLocal + MDC    │
│ └─ Retorna no header da resposta    │
└──────────────┬──────────────────────┘
               │
               │ (ThreadLocal + MDC)
               ▼
┌─────────────────────────────────────┐
│ Controller / Service                │
│ ├─ Logs incluem Correlation ID      │
│ └─ CorrelationIdUtil.getCorrelationId() │
└──────────────┬──────────────────────┘
               │
               │ (Kafka Interceptor)
               ▼
┌─────────────────────────────────────┐
│ Kafka Producer                      │
│ ├─ Correlation ID no header         │
│ └─ Propagado para consumers         │
└─────────────────────────────────────┘
```

#### Exemplo de Uso

**Requisição HTTP:**
```bash
curl -X POST http://localhost:8080/api/solicitacoes \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: meu-correlation-id-123" \
  -d '{...}'
```

**Logs:**
```
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] INFO [meu-correlation-id-123] SolicitacaoAberturaContaController - Recebida solicitação: cpf=12345678901
2024-01-15 10:30:00.456 [http-nio-8080-exec-1] INFO [meu-correlation-id-123] SolicitacaoApplicationService - Criando solicitação: cpf=12345678901
2024-01-15 10:30:00.789 [kafka-producer-1] INFO [meu-correlation-id-123] SolicitacaoKafkaPublisher - Evento publicado: solicitacaoId=1
```

**Kafka Message Headers:**
```
Headers:
  X-Correlation-Id: meu-correlation-id-123
Body:
  {"eventoId": "...", "solicitacaoId": 1, ...}
```

### Logs Estruturados

O sistema utiliza **Logback** configurado para incluir Correlation ID em todos os logs:

**logback-spring.xml:**
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

**Formato do Log:**
```
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] INFO [abc-123] Controller - Mensagem do log
```

Onde:
- `%X{correlationId:-}`: Exibe o Correlation ID do MDC, ou `-` se não existir
- Todos os logs da mesma requisição terão o mesmo Correlation ID

---

## 🎯 Princípios de Qualidade

O projeto segue rigorosamente os princípios de qualidade de software, garantindo código limpo, testável e manutenível.

### SOLID

#### Single Responsibility Principle (SRP)

Cada classe tem uma única responsabilidade:

- **Controller**: Apenas recebe requisições HTTP e delega para Application Service
- **Application Service**: Orquestra casos de uso, gerencia transações, converte DTO ↔ Model
- **Domain Service**: Contém regras de negócio puras (sem dependências de framework)
- **Repository**: Abstrai acesso a dados
- **Delegate**: Executa uma única tarefa no processo Camunda

**Exemplo:**
```java
// ✅ CORRETO: Controller apenas delega
@PostMapping
public ResponseEntity<RespostaSolicitacaoDTO> solicitarAberturaConta(
        @Valid @RequestBody final SolicitacaoAberturaContaDTO dto) {
    final SolicitacaoAberturaConta solicitacao = solicitacaoService.criarSolicitacao(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(solicitacao));
}

// ❌ ERRADO: Controller com lógica de negócio
@PostMapping
public ResponseEntity<...> solicitarAberturaConta(...) {
    if (solicitacaoService.existeContaPorCpf(dto.cpf())) { // ❌ Lógica no Controller
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    // ...
}
```

#### Open/Closed Principle (OCP)

O sistema é aberto para extensão, fechado para modificação:

- **Strategy Pattern**: Novos canais de notificação podem ser adicionados sem modificar código existente
- **Factory Pattern**: Seleção de estratégias baseada em configuração
- **Repository Interface**: Implementações podem ser trocadas sem afetar o Core

#### Liskov Substitution Principle (LSP)

Implementações são substituíveis por suas interfaces:

- `SolicitacaoServiceImpl` pode ser substituído por qualquer implementação de `ISolicitacaoService`
- `SolicitacaoRepositoryImpl` implementa `SolicitacaoRepository` sem quebrar contratos

#### Interface Segregation Principle (ISP)

Interfaces específicas e coesas:

```java
// ✅ CORRETO: Interface específica
public interface ISolicitacaoService {
    SolicitacaoAberturaConta buscarPorId(final Long id);
    SolicitacaoAberturaConta criarSolicitacao(final SolicitacaoAberturaConta solicitacao);
}

// ❌ ERRADO: Interface genérica com muitos métodos não relacionados
public interface IGenericService {
    void save(Object entity);
    void delete(Object entity);
    void update(Object entity);
    // ... muitos outros métodos
}
```

#### Dependency Inversion Principle (DIP)

Dependências apontam para abstrações:

- **Core** depende de interfaces (`ISolicitacaoService`, `SolicitacaoRepository`)
- **Infrastructure** implementa essas interfaces
- **Application** depende de abstrações do Core

```
Core (Domain)
    ↑ (depende de)
Application
    ↑ (depende de)
Infrastructure (implementa)
```

### Imutabilidade

O projeto prioriza imutabilidade para reduzir bugs e melhorar thread-safety:

#### Parâmetros e Variáveis Locais

Todos os parâmetros de métodos e variáveis locais usam `final` quando possível:

```java
// ✅ CORRETO
public SolicitacaoAberturaConta buscarPorId(final Long id) {
    final SolicitacaoAberturaConta solicitacao = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + id));
    return solicitacao;
}

// ❌ ERRADO
public SolicitacaoAberturaConta buscarPorId(Long id) {
    SolicitacaoAberturaConta solicitacao = repository.findById(id)...
    return solicitacao;
}
```

#### Domain Models

Domain Models usam `@Getter/@Setter` ao invés de `@Data` para maior controle:

```java
// ✅ CORRETO: Controle sobre mutabilidade
@Getter
@Setter
@Builder
public class SolicitacaoAberturaConta {
    private String cpf;
    private StatusSolicitacao status;
    
    // Métodos de negócio imutáveis
    public void aprovar(final String numeroConta) {
        this.status = StatusSolicitacao.APROVADA;
        this.numeroConta = numeroConta;
    }
}

// ❌ ERRADO: @Data gera setters para tudo, reduzindo controle
@Data
public class SolicitacaoAberturaConta {
    // Setters gerados para todos os campos
}
```

#### DTOs com Records

DTOs usam Java Records para imutabilidade nativa:

```java
// ✅ CORRETO: Record é imutável por padrão
public record SolicitacaoAberturaContaDTO(
        @NotBlank String cpf,
        @NotBlank String nome,
        @Email String email,
        String canal
) {}
```

### Clean Code

#### Naming Conventions

- **Classes**: Substantivos, PascalCase (`SolicitacaoAberturaConta`)
- **Métodos**: Verbos, camelCase (`criarSolicitacao`, `buscarPorId`)
- **Constantes**: UPPER_SNAKE_CASE (`MOTIVO_REJEICAO_DEFAULT`)
- **Variáveis**: camelCase, descritivas (`solicitacaoId`, `numeroConta`)

#### Magic Numbers/Strings Elimination

Todas as strings e números mágicos são substituídos por constantes ou enums:

```java
// ✅ CORRETO: Enum para type-safety
public enum Canal {
    FLAMENGO("Flamengo"),
    AZUL("Azul"),
    AMERICA("America");
    
    public static Canal getDefault() {
        return AMERICA;
    }
}

// ❌ ERRADO: Magic string
String canal = solicitacao.getCanal() != null ? solicitacao.getCanal() : "AMERICA";
```

```java
// ✅ CORRETO: Constante
private static final String MOTIVO_REJEICAO_DEFAULT = "Solicitação rejeitada durante o processo de validação";

// ❌ ERRADO: Magic string
String motivoFinal = motivoRejeicao != null ? motivoRejeicao : "Solicitação rejeitada durante o processo de validação";
```

#### Exception Handling

Exceções são tratadas de forma apropriada em cada camada:

```java
// ✅ CORRETO: Core lança exceções de negócio
public SolicitacaoAberturaConta buscarPorId(final Long id) {
    return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + id));
}

// ✅ CORRETO: Application converte para HTTP
public SolicitacaoAberturaConta buscarPorId(final Long id) {
    try {
        return solicitacaoService.buscarPorId(id);
    } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }
}
```

#### Separation of Concerns

Cada camada tem responsabilidades claras:

```
Controller (Infrastructure)
    ↓ delega
Application Service
    ↓ orquestra
Domain Service (Core)
    ↓ usa
Repository (Infrastructure)
```

**Controller**: Apenas recebe HTTP, valida entrada, delega
**Application Service**: Gerencia transações, converte DTO ↔ Model, publica eventos
**Domain Service**: Regras de negócio puras
**Repository**: Acesso a dados

### Testes

O projeto mantém alta cobertura de testes seguindo a pirâmide de testes:

#### Pirâmide de Testes

```
        /\
       /  \  E2E Tests (poucos)
      /____\
     /      \  Integration Tests
    /________\
   /          \  Unit Tests (muitos)
  /____________\
```

#### Testes Unitários (95%+ cobertura para Domain Services)

**SolicitacaoServiceImplTest.java:**
- Testa lógica de negócio pura
- Sem dependências de Spring/JPA
- Mock do Repository
- 10+ testes cobrindo todos os cenários

```java
@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceImplTest {
    @Mock
    private SolicitacaoRepository repository;
    
    @InjectMocks
    private SolicitacaoServiceImpl solicitacaoService;
    
    @Test
    void deveLancarExcecaoQuandoJaExisteContaAtiva() {
        when(repository.existsByCpfAndStatusIn(...)).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, 
                () -> solicitacaoService.criarSolicitacao(solicitacao));
    }
}
```

#### Testes de Integração

**SolicitacaoAberturaContaControllerIntegrationTest.java:**
- Testa endpoints REST
- Mock do Application Service
- Validação de HTTP status codes
- Validação de DTOs

```java
@WebMvcTest(SolicitacaoAberturaContaController.class)
class SolicitacaoAberturaContaControllerIntegrationTest {
    @Test
    void deveCriarSolicitacaoComSucesso() throws Exception {
        mockMvc.perform(post("/api/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
```

#### Cobertura de Testes

- **Domain Services**: 95%+ (objetivo)
- **Application Services**: 90%+ (objetivo)
- **Controllers**: Testes de integração cobrindo endpoints principais
- **Repositories**: Testes de integração com banco de dados

---

## 🔄 Fluxo de Processo BPMN

### Diagrama ASCII do Fluxo Principal

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PROCESSO ABERTURA CONTA PESSOA FÍSICA                │
└─────────────────────────────────────────────────────────────────────────┘

    [Início]
       │
       ▼
┌──────────────────┐
│  Validar Topaz   │ ◄─── [Error Boundary] ─── Rejeição
└────────┬─────────┘
         │
         ▼
    ┌─────────┐
    │ Gateway │ ──── Não ────► [Rejeitar]
    └────┬────┘
         │ Sim
         ▼
┌──────────────────────┐
│ Validar Antifraude  │ ◄─── [Error Boundary] ─── Rejeição
└──────────┬───────────┘
           │
           ▼
      ┌─────────┐
      │ Gateway │ ──── Não ────► [Rejeitar]
      └────┬────┘
           │ Sim
           ▼
┌──────────────────┐
│  Validar PIX     │ ◄─── [Error Boundary] ─── Rejeição
└────────┬─────────┘
         │
         ▼
    ┌─────────┐
    │ Gateway │ ──── Não ────► [Rejeitar]
    └────┬────┘
         │ Sim
         ▼
┌──────────────────┐
│ Validar Serasa   │ ◄─── [Error Boundary] ─── Rejeição
└────────┬─────────┘
         │
         ▼
    ┌─────────┐
    │ Gateway │ ──── Não ────► [Rejeitar]
    └────┬────┘
         │ Sim
         ▼
┌──────────────────────┐
│ Validar Prova de Vida│ ◄─── [Error Boundary] ─── Rejeição
└──────────┬───────────┘
           │
           ▼
      ┌─────────┐
      │ Gateway │ ──── Não ────► [Rejeitar]
      └────┬────┘
           │ Sim
           ▼
┌──────────────────────────────┐
│ Abrir Conta Sistema Interno  │ ◄─── [Error Boundary] ─── Rejeição
└──────────────┬───────────────┘
               │
               ▼
    ┌──────────────────────┐
    │ Registrar Log Sucesso│
    └──────────┬───────────┘
               │
               ▼
    [Conta Aberta com Sucesso]
```

### Fluxo Detalhado com Decisões

```
┌──────────────────────────────────────────────────────────────────────┐
│                         FLUXO COMPLETO                               │
└──────────────────────────────────────────────────────────────────────┘

1. INÍCIO
   │
   ├─► Recebe solicitação via REST API
   │
   ├─► Valida se já existe conta (CPF)
   │
   └─► Inicia processo Camunda com Business Key = ID da Solicitação

2. VALIDAÇÃO TOPAZ
   │
   ├─► Service Task: ValidarTopazDelegate
   │   ├─► Consulta score de segurança
   │   ├─► Atualiza status: VALIDANDO_TOPAZ
   │   └─► Define variável: topazAprovado (true/false)
   │
   ├─► Gateway: Topaz Aprovado?
   │   ├─► SIM → Continua para Antifraude
   │   └─► NÃO → Rejeita (Error Boundary)
   │
   └─► Error Boundary: TOPAZ_REJEITADO
       └─► Registra log e finaliza com rejeição

3. VALIDAÇÃO ANTIFRAUDE
   │
   ├─► Service Task: ValidarAntifraudeDelegate
   │   ├─► Consulta sistema antifraude
   │   ├─► Atualiza status: VALIDANDO_ANTIFRAUDE
   │   └─► Define variável: antifraudeAprovado
   │
   ├─► Gateway: Antifraude Aprovado?
   │   ├─► SIM → Continua para PIX
   │   └─► NÃO → Rejeita
   │
   └─► Error Boundary: ANTIFRAUDE_REJEITADO

4. VALIDAÇÃO PIX
   │
   ├─► Service Task: ValidarPixDelegate
   │   ├─► Consulta fraudes PIX (GPI)
   │   ├─► Atualiza status: VALIDANDO_PIX
   │   └─► Define variáveis: pixAprovado, quantidadeFraudesPix
   │
   ├─► Gateway: PIX Aprovado?
   │   ├─► SIM → Continua para Serasa
   │   └─► NÃO → Rejeita
   │
   └─► Error Boundary: PIX_REJEITADO

5. VALIDAÇÃO SERASA
   │
   ├─► Service Task: ValidarSerasaDelegate
   │   ├─► Consulta score Serasa
   │   ├─► Atualiza status: VALIDANDO_SERASA
   │   └─► Define variáveis: serasaAprovado, scoreSerasa
   │
   ├─► Gateway: Serasa Aprovado?
   │   ├─► SIM → Continua para Prova de Vida
   │   └─► NÃO → Rejeita
   │
   └─► Error Boundary: SERASA_REJEITADO

6. VALIDAÇÃO PROVA DE VIDA
   │
   ├─► Service Task: ValidarProvaVidaDelegate
   │   ├─► Validação biométrica
   │   ├─► Atualiza status: VALIDANDO_PROVA_VIDA
   │   └─► Define variáveis: provaVidaAprovado, similaridadeBiometrica
   │
   ├─► Gateway: Prova de Vida Aprovado?
   │   ├─► SIM → Continua para Abertura
   │   └─► NÃO → Rejeita
   │
   └─► Error Boundary: PROVA_VIDA_REJEITADO

7. ABERTURA DE CONTA
   │
   ├─► Service Task: AbrirContaSistemaInternoDelegate
   │   ├─► Comunica com sistema interno
   │   ├─► Gera número de conta
   │   ├─► Atualiza status: AGUARDANDO_SISTEMA_INTERNO
   │   └─► Define variáveis: numeroConta, contaAberta
   │
   ├─► Service Task: FinalizarContaAbertaDelegate
   │   ├─► Atualiza status: CONTA_ABERTA
   │   ├─► Define número da conta
   │   └─► **Publica evento no Kafka** (conta-aberta)
   │       └─► Notification Service envia notificações (Email, SMS, Push)
   │
   ├─► Service Task: RegistrarLogSucesso
   │   └─► Registra log de sucesso
   │
   └─► End Event: Conta Aberta com Sucesso

8. REJEIÇÃO
   │
   ├─► Service Task: RejeitarSolicitacaoDelegate
   │   ├─► Atualiza status: REJEITADA
   │   ├─► Define motivo da rejeição
   │   └─► **Publica evento no Kafka** (solicitacao-rejeitada)
   │       └─► Notification Service envia notificações (Email, SMS, Push)
   │
   ├─► Service Task: RegistrarLogRejeicao
   │   └─► Registra log de rejeição
   │
   └─► End Event: Solicitação Rejeitada
```

### Estados da Solicitação

```
INICIADA
    │
    ├─► VALIDANDO_TOPAZ
    │       │
    │       ├─► VALIDANDO_ANTIFRAUDE
    │       │       │
    │       │       ├─► VALIDANDO_PIX
    │       │       │       │
    │       │       │       ├─► VALIDANDO_SERASA
    │       │       │       │       │
    │       │       │       │       ├─► VALIDANDO_PROVA_VIDA
    │       │       │       │       │       │
    │       │       │       │       │       ├─► AGUARDANDO_SISTEMA_INTERNO
    │       │       │       │       │       │       │
    │       │       │       │       │       │       └─► APROVADA
    │       │       │       │       │       │               │
    │       │       │       │       │       │               └─► CONTA_ABERTA
    │       │       │       │       │       │
    │       │       │       │       │       └─► REJEITADA
    │       │       │       │       │
    │       │       │       │       └─► REJEITADA
    │       │       │       │
    │       │       │       └─► REJEITADA
    │       │       │
    │       │       └─► REJEITADA
    │       │
    │       └─► REJEITADA
    │
    └─► REJEITADA
```

---

## 🌐 Endpoints da API

### Base URL
```
http://localhost:8080/api/solicitacoes
```

### 1. Solicitar Abertura de Conta

**POST** `/api/solicitacoes`

> ⚡ **Nota**: Este endpoint retorna a resposta HTTP **imediatamente** (em ~0.7 segundos), sem aguardar o início do processo Camunda. O processo é iniciado de forma assíncrona após o commit da transação. Veja [Fluxo Assíncrono](#-fluxo-assíncrono-padrão-aco) para mais detalhes.

**Request Body:**
```json
{
  "cpf": "12345678901",
  "nome": "João Silva",
  "email": "joao.silva@email.com",
  "telefone": "11987654321"
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "cpf": "12345678901",
  "nome": "João Silva",
  "status": "INICIADA",
  "numeroConta": null,
  "motivoRejeicao": null,
  "dataCriacao": "2024-01-15T10:30:00",
  "dataAtualizacao": "2024-01-15T10:30:00"
}
```

**Tempo de Resposta**: ~0.7 segundos (não bloqueia aguardando Camunda)

**Response 409 Conflict** (conta já existe):
```json
{
  "cpf": "12345678901",
  "status": "REJEITADA",
  "motivoRejeicao": "Já existe uma conta ativa para este CPF"
}
```

### 2. Buscar Solicitação por ID

**GET** `/api/solicitacoes/{id}`

**Response 200 OK:**
```json
{
  "id": 1,
  "cpf": "12345678901",
  "nome": "João Silva",
  "status": "CONTA_ABERTA",
  "numeroConta": "00000001",
  "motivoRejeicao": null,
  "dataCriacao": "2024-01-15T10:30:00",
  "dataAtualizacao": "2024-01-15T10:35:00"
}
```

### 3. Buscar Solicitação por CPF

**GET** `/api/solicitacoes/cpf/{cpf}`

**Response 200 OK:** (mesmo formato do endpoint anterior)

**Response 404 Not Found:** (se não encontrado)

### 4. Verificar se Conta Existe

**GET** `/api/solicitacoes/cpf/{cpf}/existe`

**Response 200 OK:**
```json
true
```
ou
```json
false
```

---

## 🔧 Java Delegates

### 1. ValidarTopazDelegate

**Responsabilidade:** Validação de dispositivo e score de segurança Topaz

**Variáveis de Saída:**
- `topazAprovado` (Boolean)
- `topazScore` (Integer)

**Erros:**
- `TOPAZ_REJEITADO`: Quando validação é reprovada
- `ERRO_TOPAZ`: Quando ocorre erro na integração

### 2. ValidarAntifraudeDelegate

**Responsabilidade:** Validação antifraude

**Variáveis de Saída:**
- `antifraudeAprovado` (Boolean)

**Erros:**
- `ANTIFRAUDE_REJEITADO`: Quando validação é reprovada
- `ERRO_ANTIFRAUDE`: Quando ocorre erro na integração

### 3. ValidarPixDelegate

**Responsabilidade:** Consulta de fraudes PIX

**Variáveis de Saída:**
- `pixAprovado` (Boolean)
- `quantidadeFraudesPix` (Integer)

**Erros:**
- `PIX_REJEITADO`: Quando fraudes são detectadas
- `ERRO_PIX`: Quando ocorre erro na integração

### 4. ValidarSerasaDelegate

**Responsabilidade:** Consulta de score Serasa

**Variáveis de Saída:**
- `serasaAprovado` (Boolean)
- `scoreSerasa` (Integer)

**Erros:**
- `SERASA_REJEITADO`: Quando score é insuficiente
- `ERRO_SERASA`: Quando ocorre erro na integração

### 5. ValidarProvaVidaDelegate

**Responsabilidade:** Validação biométrica (prova de vida)

**Variáveis de Saída:**
- `provaVidaAprovado` (Boolean)
- `similaridadeBiometrica` (Double)

**Erros:**
- `PROVA_VIDA_REJEITADO`: Quando similaridade é insuficiente
- `ERRO_PROVA_VIDA`: Quando ocorre erro na integração

### 6. FinalizarContaAbertaDelegate

**Responsabilidade**: Finaliza a abertura de conta e publica evento no Kafka.

**Fluxo**:
1. Obtém o número da conta das variáveis do processo
2. Atualiza o status da solicitação para `CONTA_ABERTA`
3. Define o número da conta
4. Salva a solicitação
5. **Publica evento `ContaAbertaEvent` no Kafka** para notificar outros serviços

**Código**:
```java
var solicitacao = solicitacaoService.buscarPorId(solicitacaoId);
solicitacao.atualizarStatus(StatusSolicitacao.CONTA_ABERTA);
solicitacao.setNumeroConta(numeroConta);
solicitacaoService.salvar(solicitacao);

// Publica evento no Kafka
kafkaPublisher.publicarContaAberta(solicitacao);
```

**Evento Kafka**: `conta-aberta`
- Consumido por: `notification-service`
- Ação: Envia notificações (Email, SMS, Push) personalizadas por canal

### 7. RejeitarSolicitacaoDelegate

**Responsabilidade**: Rejeita a solicitação e publica evento no Kafka.

**Fluxo**:
1. Obtém o motivo da rejeição das variáveis do processo
2. Rejeita a solicitação com o motivo
3. Salva a solicitação
4. **Publica evento `SolicitacaoRejeitadaEvent` no Kafka** para notificar outros serviços

**Código**:
```java
var solicitacao = solicitacaoService.buscarPorId(solicitacaoId);
solicitacao.rejeitar(motivoFinal);
solicitacaoService.salvar(solicitacao);

// Publica evento no Kafka
kafkaPublisher.publicarSolicitacaoRejeitada(solicitacao);
```

**Evento Kafka**: `solicitacao-rejeitada`
- Consumido por: `notification-service`
- Ação: Envia notificações (Email, SMS, Push) informando a rejeição

### 8. AbrirContaSistemaInternoDelegate

**Responsabilidade:** Comunicação com sistema interno para abertura de conta

**Variáveis de Saída:**
- `numeroConta` (String)
- `contaAberta` (Boolean)

**Erros:**
- `ERRO_ABERTURA_CONTA`: Quando ocorre erro na abertura

### 9. RegistrarLogDelegate

**Responsabilidade:** Registro de logs de auditoria

**Variáveis de Entrada:**
- `etapa` (String)
- `resultado` (String)

### 10. AtualizarStatusDelegate

**Responsabilidade:** Atualização de status da solicitação

**Variáveis de Entrada:**
- `status` (String) - Nome do enum StatusSolicitacao

---

## 📊 Monitoramento do Camunda

### Acesso ao Cockpit do Camunda

1. **Inicie a aplicação:**
   ```bash
   mvn spring-boot:run
   ```

2. **Acesse o Cockpit:**
   ```
   http://localhost:8080/camunda/app/cockpit/default/
   ```

3. **Credenciais padrão:**
   - **Usuário:** `admin`
   - **Senha:** `admin`

### Principais Funcionalidades do Cockpit

#### 1. **Cockpit - Visão Geral de Processos**

- Visualizar processos em execução
- Ver histórico de processos finalizados
- Filtrar por processo, status, data, etc.

#### 2. **Tasklist - Tarefas Pendentes**

- Visualizar user tasks pendentes
- Completar tarefas manualmente
- Atribuir tarefas a usuários

#### 3. **Admin - Gerenciamento**

- Gerenciar usuários e grupos
- Configurar autorizações
- Visualizar métricas do sistema

### Monitoramento de Processos

#### Visualizar Instâncias de Processo

1. Acesse: `http://localhost:8080/camunda/app/cockpit/default/#/process-instance`

2. Selecione o processo: **ProcessoAberturaContaPF**

3. Visualize:
   - **Status:** Ativo, Finalizado, Cancelado
   - **Business Key:** ID da solicitação
   - **Variáveis:** Todas as variáveis do processo
   - **Histórico:** Log de execução

#### Visualizar Variáveis do Processo

1. Clique em uma instância de processo
2. Aba **"Variables"** mostra todas as variáveis:
   - `solicitacaoId`
   - `cpf`
   - `nome`
   - `topazAprovado`
   - `antifraudeAprovado`
   - `pixAprovado`
   - `serasaAprovado`
   - `provaVidaAprovado`
   - `numeroConta`
   - etc.

#### Visualizar Histórico de Execução

1. Aba **"History"** mostra:
   - Todas as atividades executadas
   - Tempo de execução de cada etapa
   - Decisões tomadas nos gateways
   - Erros ocorridos

#### Visualizar Diagrama do Processo

1. Aba **"Diagram"** mostra:
   - Fluxo completo do processo
   - Estado atual (atividades ativas destacadas)
   - Caminho percorrido

### Monitoramento via API REST do Camunda

#### Listar Instâncias de Processo

```bash
GET http://localhost:8080/engine-rest/process-instance

# Com filtros
GET http://localhost:8080/engine-rest/process-instance?processDefinitionKey=ProcessoAberturaContaPF
```

#### Buscar Instância por Business Key

```bash
GET http://localhost:8080/engine-rest/process-instance?businessKey=1
```

#### Visualizar Variáveis de uma Instância

```bash
GET http://localhost:8080/engine-rest/process-instance/{processInstanceId}/variables
```

#### Visualizar Histórico de Atividades

```bash
GET http://localhost:8080/engine-rest/history/activity-instance?processInstanceId={processInstanceId}
```

#### Visualizar Histórico de Decisões (Gateways)

```bash
GET http://localhost:8080/engine-rest/history/decision-instance?processInstanceId={processInstanceId}
```

### Monitoramento de Erros

#### Visualizar Incidentes

1. Acesse: `http://localhost:8080/camunda/app/cockpit/default/#/incident`

2. Veja todos os erros ocorridos:
   - Tipo de erro (BpmnError)
   - Mensagem de erro
   - Instância de processo afetada
   - Atividade onde ocorreu o erro

#### Retry de Processos com Erro

1. Identifique o incidente
2. Corrija a causa do erro
3. Use a API REST para retry:

```bash
POST http://localhost:8080/engine-rest/process-instance/{processInstanceId}/modification
```

### Métricas e Relatórios

#### Dashboard de Processos

1. Acesse: `http://localhost:8080/camunda/app/cockpit/default/#/dashboard`

2. Visualize:
   - Número de processos iniciados
   - Taxa de sucesso/rejeição
   - Tempo médio de execução
   - Processos mais lentos

#### Relatório de Performance

1. Aba **"Reports"** no Cockpit
2. Gere relatórios de:
   - Duração média por atividade
   - Taxa de erro por validação
   - Processos finalizados vs rejeitados

---

## 🚀 Instruções de Execução

### Pré-requisitos

- **Java 21** ou superior
- **Maven 3.6+**
- **Kafka** rodando em `localhost:9092` (para arquitetura de microserviços)
- Navegador web (para acessar Cockpit)

### 1. Compilar o Projeto

```bash
cd abertura-conta-online
mvn clean install
```

### 2. Executar a Aplicação

#### Opção 1: Executar apenas ACO Service (sem notificações)

```bash
mvn spring-boot:run
```

#### Opção 2: Executar arquitetura completa (microserviços)

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

**Nota**: Para a arquitetura completa, é necessário ter Kafka rodando. Consulte [README_MICROSERVICOS.md](./README_MICROSERVICOS.md) para mais detalhes sobre a arquitetura de microserviços.

Ou executar o JAR:

```bash
java -jar target/aco-service-1.0.0-SNAPSHOT.jar
```

### 3. Verificar se Está Rodando

#### ACO Service (porta 8080)
- API REST: `http://localhost:8080/api/solicitacoes`
- Camunda Cockpit: `http://localhost:8080/camunda/app/cockpit/default/`
  - Usuário: `admin`
  - Senha: `admin`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:aco`
  - User: `sa`
  - Password: (vazio)

#### Notification Service (porta 8081)
- Health Check: `http://localhost:8081/actuator/health`

### 4. Testar a API

#### Criar Solicitação

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

#### Consultar Solicitação

```bash
curl http://localhost:8080/api/solicitacoes/1
```

#### Verificar se Conta Existe

```bash
curl http://localhost:8080/api/solicitacoes/cpf/12345678901/existe
```

### 5. Monitorar no Camunda

1. Acesse: `http://localhost:8080/camunda/app/cockpit/default/`
2. Login: `admin` / `admin`
3. Navegue até **"Process Instances"**
4. Filtre por: `ProcessoAberturaContaPF`
5. Clique em uma instância para ver detalhes

---

## 📝 Eventos Principais do Camunda para Estudo

### 1. Start Event (Evento de Início)
- Dispara quando o processo é iniciado
- Pode receber variáveis iniciais

### 2. Service Task (Tarefa de Serviço)
- Executa código Java através de JavaDelegate
- Pode ser síncrona ou assíncrona
- Exemplo: `ValidarTopazDelegate`

### 3. Exclusive Gateway (Gateway Exclusivo)
- Ponto de decisão (XOR)
- Avalia condições e escolhe um caminho
- Exemplo: "Topaz Aprovado?"

### 4. Boundary Event (Evento de Borda)
- Captura erros ou timeouts
- Pode ser interrupting ou non-interrupting
- Exemplo: `ErrorTopaz` captura `TOPAZ_REJEITADO`

### 5. Error Event (Evento de Erro)
- Dispara quando um BpmnError é lançado
- Pode ser usado em boundary events
- Exemplo: `TOPAZ_REJEITADO`

### 6. End Event (Evento Final)
- Finaliza o processo
- Pode ter diferentes tipos (sucesso, erro, cancelamento)
- Exemplo: `EndEvent_Sucesso`, `EndEvent_Rejeicao`

### 7. Sequence Flow (Fluxo Sequencial)
- Conecta elementos do processo
- Pode ter condições (conditionExpression)
- Exemplo: `Flow_3` com condição `${topazAprovado == true}`

### 8. Business Key
- Identificador único de negócio
- Usado para correlacionar processos com entidades
- Exemplo: ID da solicitação

### 9. Process Variables (Variáveis de Processo)
- Armazenam dados durante a execução
- Acessíveis em todos os delegates
- Exemplo: `topazAprovado`, `numeroConta`

### 10. Delegate Expression
- Referência a bean Spring
- Permite injeção de dependências
- Exemplo: `${validarTopazDelegate}`

---

## 🔍 Exemplos de Uso

### Exemplo 1: Solicitação Aprovada

1. **POST** `/api/solicitacoes` → Cria solicitação ID=1
2. Processo inicia no Camunda
3. Todas as validações passam:
   - Topaz: ✅
   - Antifraude: ✅
   - PIX: ✅
   - Serasa: ✅
   - Prova de Vida: ✅
4. Conta é aberta: `numeroConta = "00000001"`
5. Status final: `CONTA_ABERTA`
6. **Evento publicado no Kafka** (`conta-aberta`)
7. **Notification Service** consome evento e envia notificações (Email, SMS, Push)

### Exemplo 2: Solicitação Rejeitada

1. **POST** `/api/solicitacoes` → Cria solicitação ID=2
2. Processo inicia no Camunda
3. Validação Topaz falha:
   - Topaz: ❌ (rejeitado)
4. Error Boundary captura erro
5. Processo finaliza com rejeição
6. Status final: `REJEITADA`
7. `motivoRejeicao = "Validação Topaz reprovada"`
8. **Evento publicado no Kafka** (`solicitacao-rejeitada`)
9. **Notification Service** consome evento e envia notificações (Email, SMS, Push)

### Exemplo 3: Monitoramento de Processo

1. Acesse Cockpit: `http://localhost:8080/camunda/app/cockpit/default/`
2. Vá em **Process Instances**
3. Filtre por `ProcessoAberturaContaPF`
4. Clique em uma instância
5. Veja:
   - **Diagram:** Fluxo atual destacado
   - **Variables:** Todas as variáveis
   - **History:** Log completo de execução
   - **Incidents:** Erros ocorridos (se houver)

---

## 📚 Referências

- [Documentação Camunda](https://docs.camunda.org/)
- [Camunda BPMN 2.0 Reference](https://docs.camunda.org/manual/7.20/reference/bpmn20/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [DDD - Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)

---

## 🎓 Conclusão

Este projeto demonstra:

✅ **Orquestração de processos** com Camunda BPM  
✅ **Padrão DDD** para organização de código  
✅ **Java Delegates** para validações assíncronas  
✅ **Tratamento de erros** com Boundary Events  
✅ **Monitoramento** através do Cockpit  
✅ **API REST** para integração  
✅ **Fluxo completo** de abertura de conta  
✅ **Observabilidade** com Correlation ID  
✅ **Arquitetura de Microserviços** com Kafka  
✅ **Princípios de Qualidade** (SOLID, Imutabilidade, Clean Code)  
✅ **Cobertura de Testes** (95%+ para Domain Services)  

**Principais aprendizados:**
- Como modelar processos BPMN 2.0
- Como implementar Java Delegates
- Como tratar erros com Boundary Events
- Como monitorar processos no Camunda
- Como estruturar projeto seguindo DDD
- Como implementar observabilidade com Correlation ID
- Como aplicar princípios SOLID e Clean Code
- Como escrever testes unitários e de integração

---

## 🚀 Melhorias Implementadas

### Observabilidade

- ✅ **Correlation ID** implementado em toda a aplicação
- ✅ **Logs estruturados** com Correlation ID via MDC
- ✅ **Propagação automática** via HTTP headers e Kafka headers
- ✅ **Rastreabilidade completa** de requisições através de múltiplos serviços

### Arquitetura DDD

- ✅ **Separação de responsabilidades**: Controller apenas delega, Application Service orquestra, Domain Service contém regras de negócio
- ✅ **Conversão DTO → Model** na camada Application (não no Controller)
- ✅ **Validações de negócio** no Domain Service (não no Controller)
- ✅ **Exception handling** apropriado: Core lança exceções de negócio, Application converte para HTTP

### Qualidade de Código

- ✅ **Imutabilidade**: `final` keywords em parâmetros e variáveis locais
- ✅ **Magic strings eliminadas**: Enum `Canal` e constantes criadas
- ✅ **SOLID principles**: Cada classe tem responsabilidade única, dependências invertidas
- ✅ **Clean Code**: Naming conventions, métodos pequenos e focados
- ✅ **Testes**: 95%+ cobertura para Domain Services, testes de integração para Controllers

### Testes

- ✅ **Testes unitários** para Domain Service (10+ testes)
- ✅ **Testes unitários** para Application Service (6+ testes)
- ✅ **Testes de integração** para Controller
- ✅ **Mocking apropriado**: Sem dependências de Spring nos testes unitários do Core

---

**Desenvolvido para estudo e demonstração de conceitos de orquestração de processos com Camunda BPM, observabilidade, e princípios de qualidade de software.**

