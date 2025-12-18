package br.com.banco.aberturaconta.infra.event;

import java.util.Map;

public class SolicitacaoCriadaEvent {
    
    private final Long solicitacaoId;
    private final Map<String, Object> variaveis;
    
    public SolicitacaoCriadaEvent(Long solicitacaoId, Map<String, Object> variaveis) {
        this.solicitacaoId = solicitacaoId;
        this.variaveis = variaveis;
    }
    
    public Long getSolicitacaoId() {
        return solicitacaoId;
    }
    
    public Map<String, Object> getVariaveis() {
        return variaveis;
    }
}