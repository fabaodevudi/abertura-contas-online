# 🔄 Fluxo Assíncrono - Padrão ACO

Este documento explica como funciona o fluxo assíncrono implementado no sistema, seguindo o padrão do projeto ACO.

> 🏗️ **Evolução para Microserviços**: O sistema foi evoluído para microserviços com Kafka. Quando o processo Camunda atinge status final (CONTA_ABERTA ou REJEITADA), eventos são publicados no Kafka e consumidos pelo Notification Service. Consulte [README_MICROSERVICOS.md](./README_MICROSERVICOS.md) para detalhes sobre a arquitetura de microserviços.

## 📋 Visão Geral

O sistema implementa uma **Arquitetura Orientada a Eventos (Event-Driven Architecture)** com múltiplas camadas de assincronismo:

1. **Spring Events** (in-process): Para iniciar o processo Camunda após commit
2. **Kafka** (distributed): Para comunicação entre microserviços quando status final é alcançado

Isso garante que a resposta HTTP seja retornada imediatamente, sem bloquear a requisição enquanto o processo Camunda é iniciado e executado, e permite que outros serviços reajam aos eventos de forma desacoplada.

## 🎯 Objetivo

- ✅ Retornar resposta HTTP em ~0.7 segundos
- ✅ Garantir que o processo Camunda só inicie após o commit da transação
- ✅ Não bloquear threads HTTP com processamento pesado
- ✅ Seguir o padrão arquitetural do ACO

## 🏗️ Arquitetura

### Arquitetura Orientada a Eventos (Event-Driven Architecture)

O sistema utiliza uma **Arquitetura Orientada a Eventos** com duas camadas de eventos:

#### 1. Eventos In-Process (Spring Events)
- **Propósito**: Coordenação interna dentro do mesmo serviço
- **Tecnologia**: Spring Application Events
- **Uso**: Iniciar processo Camunda após commit da transação
- **Características**: 
  - Síncrono ou assíncrono (usamos `@Async`)
  - Transacional (`@TransactionalEventListener` com `AFTER_COMMIT`)
  - Baixa latência (mesmo processo JVM)

#### 2. Eventos Distribuídos (Kafka)
- **Propósito**: Comunicação entre microserviços
- **Tecnologia**: Apache Kafka
- **Uso**: Notificar outros serviços sobre status final (CONTA_ABERTA ou REJEITADA)
- **Características**:
  - Assíncrono e desacoplado
  - Persistente e durável
  - Escalável e distribuído
  - Suporta múltiplos consumidores

### Componentes

1. **Controller REST** (`SolicitacaoAberturaContaController`)
2. **Service** (`SolicitacaoServiceImpl`)
3. **Evento Spring** (`SolicitacaoCriadaEvent`) - Evento in-process
4. **Listener Assíncrono** (`SolicitacaoCriadaListener`) - Processa evento Spring
5. **Processo Camunda** (`ProcessoAberturaContaPF`) - Orquestra validações
6. **Kafka Producer** (`SolicitacaoKafkaPublisher`) - Publica eventos quando status final é alcançado
7. **Kafka Consumer** (`SolicitacaoStatusFinalConsumer`) - Consome eventos no Notification Service

### Diagrama de Sequência Completo

```
Cliente    Controller    Service    Listener    Camunda    Kafka    Notification Service
   │            │            │           │          │        │              │
   │──POST /api─►│            │           │          │        │              │
   │            │            │           │          │        │              │
   │            │──salvar()─►│           │          │        │              │
   │            │            │           │          │        │              │
   │            │            │──@Transactional│     │        │              │
   │            │            │  save()    │          │        │              │
   │            │            │  publishEvent()│      │        │              │
   │            │            │  [COMMIT]  │          │        │              │
   │            │            │           │          │        │              │
   │            │◄──return───│           │          │        │              │
   │            │            │           │          │        │              │
   │◄──HTTP 201─│            │           │          │        │              │
   │  (~0.7s)   │            │           │          │        │              │
   │            │            │           │          │        │              │
   │            │            │           │──Spring Event──►│  │              │
   │            │            │           │  @Async  │        │              │
   │            │            │           │  AFTER_COMMIT│     │              │
   │            │            │           │          │        │              │
   │            │            │           │──startProcess─►│  │              │
   │            │            │           │          │        │              │
   │            │            │           │          │──Validações──►│      │
   │            │            │           │          │  (Topaz, Antifraude,│  │
   │            │            │           │          │   PIX, Serasa,      │  │
   │            │            │           │          │   Prova de Vida)    │  │
   │            │            │           │          │        │              │
   │            │            │           │          │──Status Final──►│    │
   │            │            │           │          │  (CONTA_ABERTA ou│  │
   │            │            │           │          │   REJEITADA)     │  │
   │            │            │           │          │        │              │
   │            │            │           │          │──Kafka Producer──►│  │
   │            │            │           │          │  @Async           │  │
   │            │            │           │          │  Topic: conta-aberta│ │
   │            │            │           │          │  ou solicitacao-   │  │
   │            │            │           │          │  rejeitada         │  │
   │            │            │           │          │        │              │
   │            │            │           │          │        │──Kafka Consumer──►│
   │            │            │           │          │        │  @KafkaListener│  │
   │            │            │           │          │        │  Group:        │  │
   │            │            │           │          │        │  notification- │  │
   │            │            │           │          │        │  service       │  │
   │            │            │           │          │        │              │  │
   │            │            │           │          │        │              │──NotificacaoFacade
   │            │            │           │          │        │              │  .notificarContaAberta()
   │            │            │           │          │        │              │  ou .notificarRejeitada()
   │            │            │           │          │        │              │  │
   │            │            │           │          │        │              │──Strategy Pattern
   │            │            │           │          │        │              │  (Flamengo, Azul, America)
   │            │            │           │          │        │              │  │
   │            │            │           │          │        │              │──Envia Notificações
   │            │            │           │          │        │              │  (Email, SMS, Push)
```

### Arquitetura Kafka - Visão Detalhada

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ARQUITETURA KAFKA                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│   ACO Service        │
│                      │
│  ┌────────────────┐  │
│  │ Camunda Process│  │
│  │                │  │
│  │ FinalizarConta │  │
│  │ AbertaDelegate │  │
│  └────────┬───────┘  │
│           │          │
│           │ 1. Atualiza status para CONTA_ABERTA
│           │ 2. Salva no banco
│           │ 3. Chama kafkaPublisher.publicarContaAberta()
│           │
│  ┌────────▼───────┐  │
│  │ Kafka Producer │  │
│  │ (@Async)       │  │
│  └────────┬───────┘  │
│           │          │
│           │ Publica evento
│           │
└───────────┼──────────┘
            │
            │ Kafka Message
            │ Topic: conta-aberta
            │ Key: solicitacaoId
            │ Value: ContaAbertaEvent (JSON)
            │ Headers: correlation-id
            │
            ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                              APACHE KAFKA                                  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    Topic: conta-aberta                              │  │
│  │                                                                       │  │
│  │  Partition 0: [Event1] [Event2] [Event3] ...                        │  │
│  │  Partition 1: [Event4] [Event5] ...                                │  │
│  │  Partition 2: [Event6] ...                                          │  │
│  │                                                                       │  │
│  │  Replication Factor: 3 (alta disponibilidade)                      │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │              Topic: solicitacao-rejeitada                            │  │
│  │                                                                       │  │
│  │  Partition 0: [Event1] [Event2] ...                                 │  │
│  │  Partition 1: [Event3] ...                                          │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
            │
            │ Consome eventos
            │ Consumer Group: notification-service
            │
            ▼
┌──────────────────────┐
│ Notification Service │
│                      │
│  ┌────────────────┐  │
│  │ Kafka Consumer│  │
│  │ @KafkaListener│  │
│  └────────┬───────┘  │
│           │          │
│           │ 1. Recebe evento do Kafka
│           │ 2. Extrai correlation-id
│           │ 3. Deserializa JSON para ContaAbertaEvent
│           │ 4. Chama notificacaoFacade.notificarContaAberta()
│           │
│  ┌────────▼───────┐  │
│  │ Notificacao    │  │
│  │ Facade         │  │
│  └────────┬───────┘  │
│           │          │
│           │ Usa Strategy Pattern
│           │ (Flamengo, Azul, America)
│           │
│  ┌────────▼───────┐  │
│  │ Envia:         │  │
│  │ - Email        │  │
│  │ - SMS          │  │
│  │ - Push         │  │
│  └────────────────┘  │
└──────────────────────┘
```

### Tópicos Kafka

| Tópico | Descrição | Producer | Consumer | Evento |
|--------|-----------|----------|----------|--------|
| `conta-aberta` | Evento publicado quando conta é aberta com sucesso | `SolicitacaoKafkaPublisher` | `SolicitacaoStatusFinalConsumer` | `ContaAbertaEvent` |
| `solicitacao-rejeitada` | Evento publicado quando solicitação é rejeitada | `SolicitacaoKafkaPublisher` | `SolicitacaoStatusFinalConsumer` | `SolicitacaoRejeitadaEvent` |

### Estrutura dos Eventos

#### ContaAbertaEvent
```java
{
  "eventoId": "UUID",
  "solicitacaoId": 123,
  "cpf": "12345678901",
  "nome": "João Silva",
  "email": "joao@email.com",
  "telefone": "11987654321",
  "canal": "FLAMENGO",
  "numeroConta": "12345-6",
  "dataHora": "2024-01-15T10:30:00",
  "dadosAdicionais": {}
}
```

#### SolicitacaoRejeitadaEvent
```java
{
  "eventoId": "UUID",
  "solicitacaoId": 123,
  "cpf": "12345678901",
  "nome": "João Silva",
  "email": "joao@email.com",
  "telefone": "11987654321",
  "canal": "FLAMENGO",
  "motivoRejeicao": "Score Serasa insuficiente",
  "tipoRejeicao": "SERASA",
  "dataHora": "2024-01-15T10:30:00",
  "dadosAdicionais": {}
}
```

## 📊 Diagrama ASCII Completo

> 📖 **Diagrama Detalhado:** Consulte [DIAGRAMA_FLUXO_ASSINCRONO.md](./DIAGRAMA_FLUXO_ASSINCRONO.md) para diagramas ASCII completos com timeline, fluxo de threads e comparações.

## 🎯 Arquitetura Kafka no Fluxo

### Por que Kafka?

O sistema utiliza **Apache Kafka** para comunicação entre microserviços quando o processo Camunda atinge um status final. As principais razões são:

1. **Desacoplamento**: O ACO Service não precisa conhecer o Notification Service diretamente
2. **Durabilidade**: Eventos são persistidos e podem ser reprocessados em caso de falha
3. **Escalabilidade**: Múltiplos consumidores podem processar eventos em paralelo
4. **Ordem**: Partições garantem ordem de eventos por chave (solicitacaoId)
5. **Rastreabilidade**: Correlation ID permite rastrear requisições através do sistema

### Quando os Eventos são Publicados?

Os eventos Kafka são publicados **apenas quando o processo Camunda atinge um status final**:

- **`ContaAbertaEvent`**: Publicado quando todas as validações passam e a conta é aberta
- **`SolicitacaoRejeitadaEvent`**: Publicado quando qualquer validação falha e a solicitação é rejeitada

### Fluxo Kafka Detalhado

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    FLUXO KAFKA - CONTA ABERTA                            │
└─────────────────────────────────────────────────────────────────────────┘

ACO Service (Camunda Process)
│
├─► FinalizarContaAbertaDelegate
│   │
│   ├─► 1. Atualiza status para CONTA_ABERTA
│   ├─► 2. Define número da conta
│   ├─► 3. Salva no banco
│   └─► 4. Chama kafkaPublisher.publicarContaAberta()
│       │
│       └─► SolicitacaoKafkaPublisher (@Async)
│           │
│           ├─► 5. Cria ContaAbertaEvent
│           │   ├─► eventoId (UUID)
│           │   ├─► solicitacaoId
│           │   ├─► cpf, nome, email, telefone
│           │   ├─► canal (FLAMENGO, AZUL, AMERICA)
│           │   ├─► numeroConta
│           │   └─► dataHora
│           │
│           ├─► 6. Serializa para JSON
│           │
│           └─► 7. kafkaTemplate.send()
│               ├─► Topic: "conta-aberta"
│               ├─► Key: solicitacaoId.toString()
│               ├─► Value: JSON do evento
│               └─► Headers: correlation-id
│
└─► Kafka Broker
    │
    ├─► 8. Recebe mensagem
    ├─► 9. Persiste em disco (durabilidade)
    ├─► 10. Replica para outros brokers (alta disponibilidade)
    └─► 11. Notifica consumidores
        │
        └─► Notification Service
            │
            ├─► SolicitacaoStatusFinalConsumer
            │   │
            │   ├─► 12. @KafkaListener recebe mensagem
            │   ├─► 13. Extrai correlation-id do header
            │   ├─► 14. Deserializa JSON para ContaAbertaEvent
            │   └─► 15. Chama notificacaoFacade.notificarContaAberta()
            │       │
            │       └─► NotificacaoFacadeImpl
            │           │
            │           ├─► 16. Obtém estratégia por canal (Factory)
            │           └─► 17. Envia notificações (Strategy Pattern)
            │               ├─► Email
            │               ├─► SMS
            │               └─► Push
```

### Garantias do Kafka

1. **At-Least-Once Delivery**: Mensagens são entregues pelo menos uma vez
2. **Ordering**: Mensagens com a mesma chave são processadas em ordem
3. **Durability**: Mensagens são persistidas em disco antes de confirmar
4. **Replication**: Mensagens são replicadas para alta disponibilidade
5. **Consumer Groups**: Permite escalabilidade horizontal (múltiplas instâncias)

### Tratamento de Erros

- **Producer**: Se falhar ao publicar, o erro é logado mas não interrompe o processo Camunda
- **Consumer**: Se falhar ao processar, Kafka reprocessa automaticamente (retry)
- **Idempotência**: Consumers devem ser idempotentes para evitar processamento duplicado

## 📝 Fluxo Detalhado

### 1. Requisição HTTP

```http
POST /api/solicitacoes
Content-Type: application/json

{
  "cpf": "12345678901",
  "nome": "João Silva",
  "email": "joao.silva@email.com",
  "telefone": "11987654321"
}
```

### 2. Controller Processa

```java
@PostMapping
public ResponseEntity<RespostaSolicitacaoDTO> solicitarAberturaConta(
        @Valid @RequestBody SolicitacaoAberturaContaDTO dto) {
    
    // Valida se já existe conta
    if (solicitacaoService.existeContaPorCpf(dto.cpf())) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(...);
    }
    
    // Cria e salva solicitação
    var solicitacao = SolicitacaoAberturaConta.builder()
            .cpf(dto.cpf())
            .nome(dto.nome())
            .email(dto.email())
            .telefone(dto.telefone())
            .status(StatusSolicitacao.INICIADA)
            .build();
    
    solicitacao = solicitacaoService.salvar(solicitacao);
    
    // Retorna resposta imediatamente
    return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(solicitacao));
}
```

### 3. Service Salva e Publica Evento

```java
@Service
@RequiredArgsConstructor
public class SolicitacaoServiceImpl implements ISolicitacaoService {
    
    private final SolicitacaoRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public SolicitacaoAberturaConta salvar(SolicitacaoAberturaConta solicitacao) {
        log.info("Salvando solicitação para CPF: {}", solicitacao.getCpf());
        
        // Salva no banco (dentro da transação)
        SolicitacaoAberturaConta saved = repository.save(solicitacao);
        
        // Prepara variáveis para o processo Camunda
        Map<String, Object> variaveis = Map.of(
                "solicitacaoId", saved.getId(),
                "cpf", saved.getCpf(),
                "nome", saved.getNome()
        );
        
        // Publica evento (dentro da transação)
        // O evento será processado APÓS o commit
        eventPublisher.publishEvent(
            new SolicitacaoCriadaEvent(saved.getId(), variaveis)
        );
        
        return saved;
    }
}
```

### 4. Evento Criado

```java
public class SolicitacaoCriadaEvent {
    private final Long solicitacaoId;
    private final Map<String, Object> variaveis;
    
    public SolicitacaoCriadaEvent(Long solicitacaoId, Map<String, Object> variaveis) {
        this.solicitacaoId = solicitacaoId;
        this.variaveis = variaveis;
    }
    
    // Getters...
}
```

### 5. Listener Processa Após Commit

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoCriadaListener {
    
    private static final String PROCESSO_ABERTURA = "ProcessoAberturaContaPF";
    private final RuntimeService runtimeService;
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitacaoCriadaEvent(SolicitacaoCriadaEvent event) {
        try {
            // Verifica se processo já existe
            if (!existeInstanciaCamunda(event.getSolicitacaoId().toString())) {
                log.info("Instanciando o processo {} para a solicitação {}", 
                        PROCESSO_ABERTURA, event.getSolicitacaoId());
                
                // Inicia processo Camunda de forma assíncrona
                var processInstance = runtimeService
                        .createProcessInstanceByKey(PROCESSO_ABERTURA)
                        .businessKey(event.getSolicitacaoId().toString())
                        .setVariables(event.getVariaveis())
                        .execute();
                
                log.info("✅ Processo {} instanciado com sucesso. ID: {} para solicitação: {}", 
                        PROCESSO_ABERTURA, processInstance.getId(), event.getSolicitacaoId());
            }
        } catch (Exception e) {
            log.error("❌ Erro ao instanciar processo {} para solicitação {}: {}", 
                    PROCESSO_ABERTURA, event.getSolicitacaoId(), e.getMessage(), e);
        }
    }
    
    private boolean existeInstanciaCamunda(String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .count() > 0;
    }
}
```

### 6. Processo Camunda Executa

O processo é iniciado e executa os Java Delegates sequencialmente:

1. ValidarTopazDelegate (60s delay)
2. ValidarAntifraudeDelegate (60s delay)
3. ValidarPixDelegate (60s delay)
4. ValidarSerasaDelegate (60s delay)
5. ValidarProvaVidaDelegate (60s delay)
6. AbrirContaSistemaInternoDelegate
7. **FinalizarContaAbertaDelegate** - Publica evento no Kafka

### 7. Kafka Producer - Publicação de Eventos

Quando o processo Camunda atinge status final, o `FinalizarContaAbertaDelegate` ou `RejeitarSolicitacaoDelegate` chama o Kafka Publisher:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publicarContaAberta(final SolicitacaoAberturaConta solicitacao) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        
        try {
            final String canal = obterCanal(solicitacao.getCanal());
            
            final ContaAbertaEvent event = ContaAbertaEvent.builder()
                .eventoId(UUID.randomUUID())
                .solicitacaoId(solicitacao.getId())
                .cpf(solicitacao.getCpf())
                .nome(solicitacao.getNome())
                .email(solicitacao.getEmail())
                .telefone(solicitacao.getTelefone())
                .canal(canal)
                .numeroConta(solicitacao.getNumeroConta())
                .dataHora(LocalDateTime.now())
                .build();

            final String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                KafkaTopics.CONTA_ABERTA,
                solicitacao.getId().toString(),  // Key: garante ordem
                eventJson
            );

            log.info("Evento ContaAberta publicado no Kafka: solicitacaoId={}, canal={}, correlationId={}", 
                    solicitacao.getId(), canal, correlationId);
        } catch (Exception e) {
            log.error("Erro ao publicar evento ContaAberta no Kafka: solicitacaoId={}, correlationId={}", 
                    solicitacao.getId(), correlationId, e);
        }
    }
}
```

**Características importantes:**
- `@Async`: Publicação assíncrona, não bloqueia o processo Camunda
- **Key**: `solicitacaoId.toString()` garante que eventos da mesma solicitação vão para a mesma partição
- **Correlation ID**: Propagado automaticamente via interceptor
- **Tratamento de Erros**: Loga erro mas não interrompe o processo

### 8. Kafka Consumer - Consumo de Eventos

O Notification Service consome os eventos do Kafka:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoStatusFinalConsumer {
    
    private final NotificacaoFacade notificacaoFacade;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = KafkaTopics.CONTA_ABERTA,
        groupId = "notification-service"
    )
    public void processarContaAberta(ConsumerRecord<String, String> record) {
        String correlationId = extractCorrelationId(record);
        CorrelationIdUtil.setCorrelationId(correlationId);
        
        try {
            log.info("Recebendo evento ContaAberta: correlationId={}", correlationId);
            
            ContaAbertaEvent event = objectMapper.readValue(
                record.value(), 
                ContaAbertaEvent.class
            );
            
            notificacaoFacade.notificarContaAberta(event);
            
        } catch (Exception e) {
            log.error("Erro ao processar evento ContaAberta: correlationId={}", correlationId, e);
            throw new RuntimeException("Erro ao processar evento", e);
        } finally {
            CorrelationIdUtil.clearCorrelationId();
        }
    }
    
    private String extractCorrelationId(ConsumerRecord<String, String> record) {
        // Extrai correlation-id dos headers do Kafka
        if (record.headers() != null) {
            var headers = record.headers().headers("correlation-id");
            if (headers != null && headers.iterator().hasNext()) {
                return new String(headers.iterator().next().value());
            }
        }
        return CorrelationIdUtil.getCorrelationId();
    }
}
```

**Características importantes:**
- `@KafkaListener`: Anotação Spring Kafka para consumir eventos
- **Group ID**: `notification-service` permite múltiplas instâncias processarem em paralelo
- **Correlation ID**: Extraído dos headers e usado para rastreabilidade
- **Tratamento de Erros**: Se falhar, Kafka reprocessa automaticamente

## 🔑 Anotações Importantes

### @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)

- Garante que o listener só execute **após o commit** da transação
- Evita iniciar o processo se houver rollback
- Garante que a solicitação esteja persistida antes do processo iniciar

### @Async

- Executa o listener em **thread separada**
- Não bloqueia a thread HTTP
- Permite processamento paralelo

### @Transactional no Service

- Garante que o save e publishEvent ocorram na mesma transação
- Se houver erro, tudo é revertido (rollback)
- O evento só é processado se o commit for bem-sucedido

## 📊 Tempos de Resposta

| Etapa | Tempo | Observação |
|-------|-------|------------|
| Requisição HTTP | ~0.7s | Resposta imediata |
| Salvar no banco | ~0.1s | Dentro da transação |
| Publicar evento | ~0.01s | Registro do evento |
| Commit transação | ~0.1s | Confirmação |
| **Total HTTP** | **~0.7s** | ✅ Não bloqueia |
| Iniciar processo | ~0.5s | Thread assíncrona |
| Executar delegates | ~300s | 5 × 60s (em background) |

## ✅ Vantagens da Arquitetura Orientada a Eventos

### Vantagens Gerais

1. **Performance**: Resposta HTTP rápida (~0.7s)
2. **Escalabilidade**: Não bloqueia threads HTTP
3. **Confiabilidade**: Processo só inicia após commit
4. **Resiliência**: Solicitação salva mesmo se processo falhar
5. **Padrão ACO**: Consistência arquitetural

### Vantagens Específicas do Kafka

1. **Desacoplamento**: Microserviços não precisam conhecer uns aos outros diretamente
2. **Escalabilidade Horizontal**: Múltiplos consumidores podem processar eventos em paralelo
3. **Durabilidade**: Eventos são persistidos e podem ser reprocessados
4. **Ordem de Mensagens**: Partições garantem ordem por chave (solicitacaoId)
5. **Correlation ID**: Rastreabilidade completa através do sistema
6. **Tolerância a Falhas**: Se um consumidor falhar, Kafka reprocessa automaticamente
7. **Extensibilidade**: Fácil adicionar novos consumidores sem modificar producers

### Padrões de Arquitetura Orientada a Eventos

#### 1. Event Sourcing (Parcial)
- Eventos representam fatos que aconteceram no sistema
- Eventos são imutáveis e auditáveis
- Permite reconstruir estado através dos eventos

#### 2. Pub/Sub (Publish/Subscribe)
- Producers publicam eventos sem conhecer consumidores
- Múltiplos consumidores podem processar o mesmo evento
- Desacoplamento temporal (consumidor pode estar offline)

#### 3. CQRS (Command Query Responsibility Segregation)
- Comandos (writes) separados de queries (reads)
- Eventos permitem atualizar múltiplas read models
- Notification Service é uma read model atualizada por eventos

#### 4. Saga Pattern
- Processo distribuído através de múltiplos serviços
- Cada etapa publica eventos para próxima etapa
- Compensação através de eventos de rollback (se necessário)

## 🔍 Monitoramento

### Logs

```bash
# Ver logs do processo
docker logs abertura-conta-online | grep -E "(Solicitação|Instanciando|instanciado)"

# Exemplo de saída:
INFO - Solicitação 1 criada. Processo Camunda será iniciado assincronamente após commit.
INFO - Instanciando o processo ProcessoAberturaContaPF para a solicitação 1
INFO - ✅ Processo ProcessoAberturaContaPF instanciado com sucesso. ID: xxx para solicitação: 1
```

### Camunda Cockpit

1. Acesse: http://localhost:8080/camunda/app/cockpit/default/
2. Vá em **Process Instances**
3. Filtre por: `ProcessoAberturaContaPF`
4. Veja instâncias em execução ou completadas

### API REST do Camunda

```bash
# Listar instâncias ativas
curl "http://localhost:8080/engine-rest/process-instance?processDefinitionKey=ProcessoAberturaContaPF"

# Listar histórico
curl "http://localhost:8080/engine-rest/history/process-instance?processDefinitionKey=ProcessoAberturaContaPF"
```

## 🐛 Troubleshooting

### Processo não inicia

1. Verifique se o `@EnableAsync` está habilitado
2. Verifique logs do listener
3. Verifique se o evento está sendo publicado
4. Verifique se há erros na transação

### Processo inicia antes do commit

- Verifique se está usando `TransactionPhase.AFTER_COMMIT`
- Verifique se o evento é publicado dentro da transação

### Resposta HTTP lenta

- Verifique se não há bloqueios no service
- Verifique se o banco não está lento
- Verifique logs para identificar gargalos

## 🔧 Configuração Kafka

### Producer Configuration

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Interceptor para adicionar correlation-id
        configProps.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, 
                KafkaCorrelationIdInterceptor.class.getName());
        
        // Garantir que mensagens sejam replicadas
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
}
```

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      enable-auto-commit: false
    listener:
      ack-mode: manual
```

### Correlation ID

O sistema utiliza **Correlation ID** para rastrear requisições através de múltiplos serviços:

1. **HTTP Request**: Correlation ID é gerado no filtro HTTP
2. **Spring Event**: Correlation ID é propagado na thread
3. **Kafka Producer**: Correlation ID é adicionado como header
4. **Kafka Consumer**: Correlation ID é extraído do header e usado nos logs

Isso permite rastrear uma solicitação desde a requisição HTTP até as notificações finais.

## 📊 Fluxo de Dados Completo

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    FLUXO DE EVENTOS COMPLETO                             │
└─────────────────────────────────────────────────────────────────────────┘

1. HTTP Request
   └─► Controller
       └─► Service (@Transactional)
           ├─► Salva no banco
           └─► Publica Spring Event (SolicitacaoCriadaEvent)
               └─► [COMMIT TRANSAÇÃO]
                   └─► Listener (@Async + AFTER_COMMIT)
                       └─► Inicia Processo Camunda
                           │
                           ├─► Validações (Topaz, Antifraude, PIX, Serasa, Prova de Vida)
                           │
                           ├─► Status Final Alcançado
                           │   │
                           │   ├─► CONTA_ABERTA
                           │   │   └─► Kafka Producer
                           │   │       └─► Topic: conta-aberta
                           │   │           └─► ContaAbertaEvent
                           │   │
                           │   └─► REJEITADA
                           │       └─► Kafka Producer
                           │           └─► Topic: solicitacao-rejeitada
                           │               └─► SolicitacaoRejeitadaEvent
                           │
                           └─► Kafka Broker
                               └─► Persiste eventos
                                   └─► Kafka Consumer (Notification Service)
                                       └─► NotificacaoFacade
                                           └─► Strategy Pattern
                                               └─► Envia Notificações
                                                   ├─► Email
                                                   ├─► SMS
                                                   └─► Push
```

## 📚 Referências

### Spring Events
- [Spring Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Transactional Event Listeners](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html#transaction-event-listener)
- [Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)

### Apache Kafka
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/html/)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#producerconfigs)

### Arquitetura Orientada a Eventos
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

