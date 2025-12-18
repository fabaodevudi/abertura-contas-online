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
public class ValidarProvaVidaDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando validação Prova de Vida para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando validação Prova de Vida (aguarde 60 segundos)...");
            Thread.sleep(60000);
            
            var aprovado = validarProvaVida(solicitacaoId);
            var similaridade = aprovado ? 0.95 : 0.60;
            
            execution.setVariable("provaVidaAprovado", aprovado);
            execution.setVariable("similaridadeBiometrica", similaridade);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.VALIDANDO_PROVA_VIDA);
            
            if (!aprovado) {
                log.warn("Solicitação {} rejeitada na Prova de Vida - Similaridade: {}", solicitacaoId, similaridade);
                execution.setVariable("motivoRejeicao", "PROVA_VIDA - A análise de documentos e selfie não foi aprovada. Por favor, tente novamente com documentos válidos e selfie nítida");
                throw new BpmnError("PROVA_VIDA_REJEITADO", "Validação Prova de Vida reprovada - similaridade insuficiente");
            }
            
            log.info("Validação Prova de Vida aprovada para solicitação: {} - Similaridade: {}", solicitacaoId, similaridade);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro na validação Prova de Vida para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_PROVA_VIDA", e);
        }
    }

    private boolean validarProvaVida(Long solicitacaoId) {
        
        return Math.random() > 0.1;
    }
}