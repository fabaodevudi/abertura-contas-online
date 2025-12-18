package br.com.banco.aberturaconta.infra.repository.impl;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.repository.SolicitacaoRepository;
import br.com.banco.aberturaconta.infra.entity.SolicitacaoAberturaContaData;
import br.com.banco.aberturaconta.infra.mapper.SolicitacaoAberturaContaMapper;
import br.com.banco.aberturaconta.infra.repository.jpa.SolicitacaoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SolicitacaoRepositoryImpl implements SolicitacaoRepository {

    private final SolicitacaoJpaRepository jpaRepository;
    private final SolicitacaoAberturaContaMapper mapper = SolicitacaoAberturaContaMapper.INSTANCE;
    
    @Override
    public Optional<SolicitacaoAberturaConta> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toModel);  
    }
    
    @Override
    public Optional<SolicitacaoAberturaConta> findByCpf(String cpf) {
        return jpaRepository.findByCpf(cpf)
                .map(mapper::toModel);  
    }
    
    @Override
    public boolean existsByCpfAndStatusIn(String cpf, List<StatusSolicitacao> status) {
        return jpaRepository.existsByCpfAndStatusIn(cpf, status);
    }
    
    @Override
    public SolicitacaoAberturaConta save(SolicitacaoAberturaConta solicitacao) {
        SolicitacaoAberturaContaData data = mapper.toData(solicitacao);  
        SolicitacaoAberturaContaData saved = jpaRepository.save(data); 
        return mapper.toModel(saved);                 
    }
}