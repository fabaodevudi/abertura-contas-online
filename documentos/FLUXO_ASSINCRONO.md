# 🔄 Fluxo Assíncrono - Padrão ACO

Este documento explica como funciona o fluxo assíncrono implementado no sistema, seguindo o padrão do projeto ACO.

> 🏗️ **Evolução para Microserviços**: O sistema foi evoluído para microserviços com Kafka. Quando o processo Camunda atinge status final (CONTA_ABERTA ou REJEITADA), eventos são publicados no Kafka e consumidos pelo Notification Service. Consulte [README_MICROSERVICOS.md](./README_MICROSERVICOS.md) para detalhes sobre a arquitetura de microserviços.

## 📋 Visão Geral

O sistema implementa um padrão de **eventos assíncronos** para garantir que a resposta HTTP seja retornada imediatamente, sem bloquear a requisição enquanto o processo Camunda é iniciado e executado.

## 🎯 Objetivo

- ✅ Retornar resposta HTTP em ~0.7 segundos
- ✅ Garantir que o processo Camunda só inicie após o commit da transação
- ✅ Não bloquear threads HTTP com processamento pesado
- ✅ Seguir o padrão arquitetural do ACO

## 🏗️ Arquitetura

### Componentes

1. **Controller REST** (`SolicitacaoAberturaContaController`)
2. **Service** (`SolicitacaoServiceImpl`)
3. **Evento** (`SolicitacaoCriadaEvent`)
4. **Listener Assíncrono** (`SolicitacaoCriadaListener`)
5. **Processo Camunda** (`ProcessoAberturaContaPF`)
6. **Kafka Producer** (`SolicitacaoKafkaPublisher`) - Publica eventos quando status final é alcançado
7. **Kafka Consumer** (`SolicitacaoStatusFinalConsumer`) - Consome eventos no Notification Service

### Diagrama de Sequência

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
   │            │            │           │──Event──►│        │              │
   │            │            │           │  @Async  │        │              │
   │            │            │           │  AFTER_COMMIT│     │              │
   │            │            │           │          │        │              │
   │            │            │           │──startProcess─►│  │              │
   │            │            │           │          │        │              │
   │            │            │           │          │──Validações──►│      │
   │            │            │           │          │        │              │
   │            │            │           │          │──Status Final──►│    │
   │            │            │           │          │        │              │
   │            │            │           │          │──Publica Evento──►│  │
   │            │            │           │          │        │              │
   │            │            │           │          │        │──Consume──►│
   │            │            │           │          │        │              │
   │            │            │           │          │        │              │──Envia Notificações
   │            │            │           │          │        │              │  (Email, SMS, Push)
   │                 │                │                 │                 │──Executa
   │                 │                │                 │                 │  Delegates
```

## 📊 Diagrama ASCII Completo

> 📖 **Diagrama Detalhado:** Consulte [DIAGRAMA_FLUXO_ASSINCRONO.md](./DIAGRAMA_FLUXO_ASSINCRONO.md) para diagramas ASCII completos com timeline, fluxo de threads e comparações.

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

## ✅ Vantagens

1. **Performance**: Resposta HTTP rápida (~0.7s)
2. **Escalabilidade**: Não bloqueia threads HTTP
3. **Confiabilidade**: Processo só inicia após commit
4. **Resiliência**: Solicitação salva mesmo se processo falhar
5. **Padrão ACO**: Consistência arquitetural

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

## 📚 Referências

- [Spring Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Transactional Event Listeners](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html#transaction-event-listener)
- [Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)

