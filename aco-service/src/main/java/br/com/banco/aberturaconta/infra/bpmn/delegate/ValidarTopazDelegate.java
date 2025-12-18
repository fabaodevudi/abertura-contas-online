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
public class ValidarTopazDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando validação Topaz para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando validação Topaz (aguarde 60 segundos)...");
            Thread.sleep(60000);

            var aprovado = validarTopaz(solicitacaoId);
            
            execution.setVariable("topazAprovado", aprovado);
            execution.setVariable("topazScore", aprovado ? 85 : 30);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.VALIDANDO_TOPAZ);
            
            if (!aprovado) {
                log.warn("Solicitação {} rejeitada no Topaz", solicitacaoId);
                execution.setVariable("motivoRejeicao", "TOPAZ - Problemas relacionados ao dispositivo durante a análise de segurança");
                throw new BpmnError("TOPAZ_REJEITADO", "Validação Topaz reprovada - problemas com dispositivo");
            }
            
            log.info("Validação Topaz aprovada para solicitação: {}", solicitacaoId);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro na validação Topaz para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_TOPAZ", e);
        }
    }

    private boolean validarTopaz(Long solicitacaoId) {
        
        return Math.random() > 0.2;
    }
}