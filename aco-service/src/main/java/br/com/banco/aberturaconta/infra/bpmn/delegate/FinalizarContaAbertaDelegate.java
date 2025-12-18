package br.com.banco.aberturaconta.infra.bpmn.delegate;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.infra.kafka.SolicitacaoKafkaPublisher;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizarContaAbertaDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;
    private final SolicitacaoKafkaPublisher kafkaPublisher;

    @Override
    public void execute(final DelegateExecution execution) {
        final Long solicitacaoId = Long.parseLong(execution.getBusinessKey());
        final String numeroConta = (String) execution.getVariable("numeroConta");
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        
        log.info("Finalizando abertura de conta: solicitacaoId={}, numeroConta={}, correlationId={}", 
                solicitacaoId, numeroConta, correlationId);
        
        final SolicitacaoAberturaConta solicitacao = solicitacaoService.buscarPorId(solicitacaoId);
        solicitacao.aprovar(numeroConta);
        solicitacao.atualizarStatus(StatusSolicitacao.CONTA_ABERTA);
        solicitacaoService.salvar(solicitacao);

        kafkaPublisher.publicarContaAberta(solicitacao);
    }
}