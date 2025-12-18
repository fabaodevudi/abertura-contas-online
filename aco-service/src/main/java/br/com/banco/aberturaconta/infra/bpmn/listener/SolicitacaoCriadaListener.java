package br.com.banco.aberturaconta.infra.bpmn.listener;

import br.com.banco.aberturaconta.infra.event.SolicitacaoCriadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoCriadaListener {
    
    private static final String PROCESSO_ABERTURA = "ProcessoAberturaContaPF";
    
    private final RuntimeService runtimeService;
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = SolicitacaoCriadaEvent.class)
    public void onSolicitacaoCriadaEvent(SolicitacaoCriadaEvent event) {
        try {
            if (!existeInstanciaCamunda(event.getSolicitacaoId().toString())) {
                log.info("Instanciando o processo {} para a solicitação {}", PROCESSO_ABERTURA, event.getSolicitacaoId());
                
                var processInstance = runtimeService
                        .createProcessInstanceByKey(PROCESSO_ABERTURA)
                        .businessKey(event.getSolicitacaoId().toString())
                        .setVariables(event.getVariaveis())
                        .execute();
                
                log.info("✅ Processo {} instanciado com sucesso. ID: {} para solicitação: {}", 
                        PROCESSO_ABERTURA, processInstance.getId(), event.getSolicitacaoId());
            } else {
                log.warn("Processo {} já existe para a solicitação {}", PROCESSO_ABERTURA, event.getSolicitacaoId());
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