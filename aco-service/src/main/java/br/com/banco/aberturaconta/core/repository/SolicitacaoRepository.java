package br.com.banco.aberturaconta.core.repository;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoRepository {
    
    Optional<SolicitacaoAberturaConta> findById(Long id);
    
    Optional<SolicitacaoAberturaConta> findByCpf(String cpf);
    
    boolean existsByCpfAndStatusIn(String cpf, List<StatusSolicitacao> status);
    
    SolicitacaoAberturaConta save(SolicitacaoAberturaConta solicitacao);
}