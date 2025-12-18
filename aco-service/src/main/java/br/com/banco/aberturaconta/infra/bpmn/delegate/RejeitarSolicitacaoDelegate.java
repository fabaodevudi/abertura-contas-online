package br.com.banco.aberturaconta.infra.bpmn.delegate;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
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
public class RejeitarSolicitacaoDelegate implements JavaDelegate {

    private static final String MOTIVO_REJEICAO_DEFAULT = "Solicitação rejeitada durante o processo de validação";

    private final SolicitacaoApplicationService solicitacaoService;
    private final SolicitacaoKafkaPublisher kafkaPublisher;

    @Override
    public void execute(final DelegateExecution execution) {
        final Long solicitacaoId = Long.parseLong(execution.getBusinessKey());
        final String motivoRejeicao = (String) execution.getVariable("motivoRejeicao");
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        
        final String motivoFinal = obterMotivoFinal(motivoRejeicao);
        
        log.warn("Rejeitando solicitação: solicitacaoId={}, motivo={}, correlationId={}", 
                solicitacaoId, motivoFinal, correlationId);
        
        final SolicitacaoAberturaConta solicitacao = solicitacaoService.buscarPorId(solicitacaoId);
        solicitacao.rejeitar(motivoFinal);
        solicitacaoService.salvar(solicitacao);

        kafkaPublisher.publicarSolicitacaoRejeitada(solicitacao);
    }

    private String obterMotivoFinal(final String motivoRejeicao) {
        return motivoRejeicao != null && !motivoRejeicao.isBlank() 
                ? motivoRejeicao 
                : MOTIVO_REJEICAO_DEFAULT;
    }
}