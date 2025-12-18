package br.com.banco.aberturaconta.infra.repository.jpa;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.infra.entity.SolicitacaoAberturaContaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoJpaRepository extends JpaRepository<SolicitacaoAberturaContaData, Long> {
    
    Optional<SolicitacaoAberturaContaData> findByCpf(String cpf);
    
    boolean existsByCpfAndStatusIn(String cpf, List<StatusSolicitacao> status);
}