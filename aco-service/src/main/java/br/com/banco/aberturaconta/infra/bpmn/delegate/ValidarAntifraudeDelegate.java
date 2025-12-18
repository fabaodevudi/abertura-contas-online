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
public class ValidarAntifraudeDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando validação Antifraude para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando validação Antifraude (aguarde 60 segundos)...");
            Thread.sleep(60000);
            
            var aprovado = validarAntifraude(solicitacaoId);
            
            execution.setVariable("antifraudeAprovado", aprovado);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.VALIDANDO_ANTIFRAUDE);
            
            if (!aprovado) {
                log.warn("Solicitação {} rejeitada no Antifraude", solicitacaoId);
                execution.setVariable("motivoRejeicao", "ANTIFRAUDE - Sua solicitação não passou na análise antifraude");
                throw new BpmnError("ANTIFRAUDE_REJEITADO", "Validação Antifraude reprovada");
            }
            
            log.info("Validação Antifraude aprovada para solicitação: {}", solicitacaoId);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro na validação Antifraude para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_ANTIFRAUDE", e);
        }
    }

    private boolean validarAntifraude(Long solicitacaoId) {
        
        return Math.random() > 0.15;
    }
}