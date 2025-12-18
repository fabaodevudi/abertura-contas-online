package br.com.banco.aberturaconta.application.service;

import br.com.banco.aberturaconta.core.domain.Canal;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.service.ISolicitacaoService;
import br.com.banco.aberturaconta.infra.dto.SolicitacaoAberturaContaDTO;
import br.com.banco.aberturaconta.infra.event.SolicitacaoCriadaEvent;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitacaoApplicationService {

    private final ISolicitacaoService solicitacaoService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional  
    public SolicitacaoAberturaConta criarSolicitacao(final SolicitacaoAberturaContaDTO dto) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.info("Criando solicitação: cpf={}, correlationId={}", dto.cpf(), correlationId);
        
        try {
            
            final SolicitacaoAberturaConta solicitacao = toDomainModel(dto);

            final SolicitacaoAberturaConta saved = solicitacaoService.criarSolicitacao(solicitacao);

            publishSolicitacaoCriadaEvent(saved);
            
            log.info("Solicitação {} criada. Processo Camunda será iniciado assincronamente após commit. correlationId={}", 
                    saved.getId(), correlationId);
            
            return saved;
            
        } catch (IllegalArgumentException e) {
            
            if (e.getMessage().contains("Já existe uma conta ativa")) {
                log.warn("Tentativa de criar solicitação para CPF com conta ativa: cpf={}, correlationId={}", 
                        dto.cpf(), correlationId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
            }
            log.error("Erro ao criar solicitação: cpf={}, correlationId={}", dto.cpf(), correlationId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @Transactional  
    public SolicitacaoAberturaConta salvar(final SolicitacaoAberturaConta solicitacao) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.info("Salvando solicitação: cpf={}, correlationId={}", solicitacao.getCpf(), correlationId);

        final SolicitacaoAberturaConta saved = solicitacaoService.salvar(solicitacao);

        publishSolicitacaoCriadaEvent(saved);
        
        log.info("Solicitação {} salva. correlationId={}", saved.getId(), correlationId);
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public SolicitacaoAberturaConta buscarPorId(final Long id) {
        try {
            return solicitacaoService.buscarPorId(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public SolicitacaoAberturaConta buscarPorCpf(final String cpf) {
        final SolicitacaoAberturaConta solicitacao = solicitacaoService.buscarPorCpf(cpf);
        if (solicitacao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada para CPF: " + cpf);
        }
        return solicitacao;
    }
    
    @Transactional
    public void atualizarStatus(final Long id, final StatusSolicitacao status) {
        try {
            solicitacaoService.atualizarStatus(id, status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public boolean existeContaPorCpf(final String cpf) {
        return solicitacaoService.existeContaPorCpf(cpf);
    }

    private SolicitacaoAberturaConta toDomainModel(final SolicitacaoAberturaContaDTO dto) {
        return SolicitacaoAberturaConta.builder()
                .cpf(dto.cpf())
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .canal(dto.canal() != null ? Canal.fromString(dto.canal()).name() : Canal.getDefault().name())
                .status(StatusSolicitacao.INICIADA)
                .build();
    }

    private void publishSolicitacaoCriadaEvent(final SolicitacaoAberturaConta solicitacao) {
        final Map<String, Object> variaveis = Map.of(
                "solicitacaoId", solicitacao.getId(),
                "cpf", solicitacao.getCpf(),
                "nome", solicitacao.getNome()
        );
        eventPublisher.publishEvent(new SolicitacaoCriadaEvent(solicitacao.getId(), variaveis));
    }
}