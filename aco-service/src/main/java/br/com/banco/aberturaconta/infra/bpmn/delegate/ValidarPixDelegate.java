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
public class ValidarPixDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando validação PIX para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando validação PIX (aguarde 60 segundos)...");
            Thread.sleep(60000);
            
            var aprovado = validarPix(solicitacaoId);
            var quantidadeFraudes = aprovado ? 0 : 3;
            
            execution.setVariable("pixAprovado", aprovado);
            execution.setVariable("quantidadeFraudesPix", quantidadeFraudes);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.VALIDANDO_PIX);
            
            if (!aprovado) {
                log.warn("Solicitação {} rejeitada no PIX - {} fraudes encontradas", solicitacaoId, quantidadeFraudes);
                execution.setVariable("motivoRejeicao", "PIX - Identificamos pendências relacionadas ao PIX durante a análise");
                throw new BpmnError("PIX_REJEITADO", "Validação PIX reprovada - fraudes detectadas");
            }
            
            log.info("Validação PIX aprovada para solicitação: {}", solicitacaoId);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro na validação PIX para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_PIX", e);
        }
    }

    private boolean validarPix(Long solicitacaoId) {
        
        return Math.random() > 0.1;
    }
}