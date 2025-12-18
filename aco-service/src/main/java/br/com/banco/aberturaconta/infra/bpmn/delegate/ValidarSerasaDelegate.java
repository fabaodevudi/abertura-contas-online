package br.com.banco.aberturaconta.infra.bpmn.delegate;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidarSerasaDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando validação Serasa para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando validação Serasa (aguarde 60 segundos)...");
            Thread.sleep(60000);
            
            var aprovado = validarSerasa(solicitacaoId);
            var scoreSerasa = aprovado ? 750 : 400;
            
            execution.setVariable("serasaAprovado", aprovado);
            execution.setVariable("scoreSerasa", scoreSerasa);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.VALIDANDO_SERASA);
            
            if (!aprovado) {
                log.warn("Solicitação {} rejeitada no Serasa - Score: {}", solicitacaoId, scoreSerasa);
                execution.setVariable("motivoRejeicao", "SERASA - Pendências no Serasa que precisam ser regularizadas antes de abrir sua conta");
                throw new BpmnError("SERASA_REJEITADO", "Validação Serasa reprovada - score insuficiente");
            }
            
            log.info("Validação Serasa aprovada para solicitação: {} - Score: {}", solicitacaoId, scoreSerasa);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro na validação Serasa para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_SERASA", e);
        }
    }

    private boolean validarSerasa(Long solicitacaoId) {
        
        return Math.random() > 0.25;
    }
}