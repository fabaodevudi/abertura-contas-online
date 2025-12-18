package br.com.banco.aberturaconta.infra.bpmn.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrarLogDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        var solicitacaoId = Long.parseLong(execution.getBusinessKey());
        var etapa = (String) execution.getVariable("etapa");
        var resultado = (String) execution.getVariable("resultado");
        
        log.info("""
                === LOG DE PROCESSO ===
                Solicitação: {}
                Etapa: {}
                Resultado: {}
                ======================
                """, solicitacaoId, etapa, resultado);
    }
}