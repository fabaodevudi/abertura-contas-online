package br.com.banco.aberturaconta.core.service;

import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SolicitacaoServiceImpl implements ISolicitacaoService {

    private final SolicitacaoRepository repository;

    @Override
    
    public SolicitacaoAberturaConta buscarPorId(final Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + id));
    }

    @Override
    
    public SolicitacaoAberturaConta buscarPorCpf(final String cpf) {
        return repository.findByCpf(cpf)
                .orElse(null);
    }

    @Override
    
    public SolicitacaoAberturaConta salvar(final SolicitacaoAberturaConta solicitacao) {
        log.info("Salvando solicitação para CPF: {}", solicitacao.getCpf());
        return repository.save(solicitacao);
    }

    @Override
    
    public void atualizarStatus(final Long id, final StatusSolicitacao status) {
        log.info("Atualizando status da solicitação {} para {}", id, status);
        final SolicitacaoAberturaConta solicitacao = buscarPorId(id);
        solicitacao.atualizarStatus(status);
        repository.save(solicitacao);
    }

    @Override
    
    public boolean existeContaPorCpf(final String cpf) {
        return repository.existsByCpfAndStatusIn(
                cpf, 
                List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA)
        );
    }
    
    @Override
    
    public SolicitacaoAberturaConta criarSolicitacao(final SolicitacaoAberturaConta solicitacao) {
        
        if (existeContaPorCpf(solicitacao.getCpf())) {
            throw new IllegalArgumentException("Já existe uma conta ativa para este CPF: " + solicitacao.getCpf());
        }
        
        log.info("Criando nova solicitação para CPF: {}", solicitacao.getCpf());
        return salvar(solicitacao);
    }
}