package br.com.banco.aberturaconta.core.service;

import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;

public interface ISolicitacaoService {
    
    SolicitacaoAberturaConta buscarPorId(final Long id);
    
    SolicitacaoAberturaConta buscarPorCpf(final String cpf);
    
    SolicitacaoAberturaConta salvar(final SolicitacaoAberturaConta solicitacao);
    
    void atualizarStatus(final Long id, final StatusSolicitacao status);
    
    boolean existeContaPorCpf(final String cpf);

    SolicitacaoAberturaConta criarSolicitacao(final SolicitacaoAberturaConta solicitacao);
}