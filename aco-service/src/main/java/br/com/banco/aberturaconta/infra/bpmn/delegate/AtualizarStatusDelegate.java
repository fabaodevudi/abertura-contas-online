package br.com.banco.aberturaconta.infra.bpmn.delegate;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtualizarStatusDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        var statusStr = (String) execution.getVariable("status");
        
        if (statusStr != null) {
            try {
                var status = StatusSolicitacao.valueOf(statusStr);
                log.info("Atualizando status da solicitação {} para {}", solicitacaoId, status);
                solicitacaoService.atualizarStatus(solicitacaoId, status);
            } catch (IllegalArgumentException e) {
                log.warn("Status inválido para solicitação {}: {}", solicitacaoId, statusStr);
            }
        }
    }
}