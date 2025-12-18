package br.com.banco.aberturaconta.infra.bpmn.delegate;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbrirContaSistemaInternoDelegate implements JavaDelegate {

    private final SolicitacaoApplicationService solicitacaoService;

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        log.info("Iniciando abertura de conta no sistema interno para solicitação: {}", solicitacaoId);

        try {
            
            log.info("⏳ Processando abertura de conta no sistema interno (aguarde 60 segundos)...");
            Thread.sleep(60000);
            
            var numeroConta = abrirContaNoSistemaInterno(solicitacaoId);
            
            execution.setVariable("numeroConta", numeroConta);
            execution.setVariable("contaAberta", true);
            
            solicitacaoService.atualizarStatus(solicitacaoId, StatusSolicitacao.AGUARDANDO_SISTEMA_INTERNO);

            var solicitacao = solicitacaoService.buscarPorId(solicitacaoId);
            solicitacao.aprovar(numeroConta);
            solicitacaoService.salvar(solicitacao);
            
            log.info("Conta aberta com sucesso no sistema interno para solicitação: {} - Conta: {}", solicitacaoId, numeroConta);
            
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao abrir conta no sistema interno para solicitação {}: {}", solicitacaoId, e.getMessage(), e);
            throw new BpmnError("ERRO_ABERTURA_CONTA", e);
        }
    }

    private String abrirContaNoSistemaInterno(Long solicitacaoId) {
        
        var numeroConta = String.format("%08d", solicitacaoId);
        log.info("Conta gerada no sistema interno: {}", numeroConta);
        return numeroConta;
    }
}