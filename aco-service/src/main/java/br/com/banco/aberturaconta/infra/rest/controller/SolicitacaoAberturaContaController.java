package br.com.banco.aberturaconta.infra.rest.controller;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.infra.dto.RespostaSolicitacaoDTO;
import br.com.banco.aberturaconta.infra.dto.SolicitacaoAberturaContaDTO;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
@Tag(name = "Solicitações de Abertura de Conta", description = "API para gerenciamento de solicitações de abertura de conta online")
public class SolicitacaoAberturaContaController {

    private final SolicitacaoApplicationService solicitacaoService;

    @Operation(
            summary = "Criar nova solicitação de abertura de conta",
            description = "Cria uma nova solicitação de abertura de conta e inicia o processo de validação no Camunda BPM"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Solicitação criada com sucesso",
                    content = @Content(schema = @Schema(implementation = RespostaSolicitacaoDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe uma conta ativa para este CPF",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<RespostaSolicitacaoDTO> solicitarAberturaConta(
            @Parameter(description = "Dados da solicitação de abertura de conta", required = true)
            @Valid @RequestBody final SolicitacaoAberturaContaDTO dto) {
        
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.info("Recebida solicitação de abertura de conta: cpf={}, correlationId={}", dto.cpf(), correlationId);

        final SolicitacaoAberturaConta solicitacao = solicitacaoService.criarSolicitacao(dto);
        
        log.info("Solicitação {} criada. Processo Camunda será iniciado assincronamente após commit. correlationId={}", 
                solicitacao.getId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(solicitacao));
    }

    @Operation(
            summary = "Buscar solicitação por ID",
            description = "Retorna os dados de uma solicitação de abertura de conta pelo seu ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Solicitação encontrada",
                    content = @Content(schema = @Schema(implementation = RespostaSolicitacaoDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitação não encontrada",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<RespostaSolicitacaoDTO> buscarSolicitacao(
            @Parameter(description = "ID da solicitação", required = true)
            @PathVariable final Long id) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.debug("Buscando solicitação por ID: id={}, correlationId={}", id, correlationId);
        
        final SolicitacaoAberturaConta solicitacao = solicitacaoService.buscarPorId(id);
        return ResponseEntity.ok(toDTO(solicitacao));
    }

    @Operation(
            summary = "Buscar solicitação por CPF",
            description = "Retorna os dados de uma solicitação de abertura de conta pelo CPF"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Solicitação encontrada",
                    content = @Content(schema = @Schema(implementation = RespostaSolicitacaoDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitação não encontrada",
                    content = @Content
            )
    })
    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<RespostaSolicitacaoDTO> buscarPorCpf(
            @Parameter(description = "CPF do solicitante", required = true)
            @PathVariable final String cpf) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.debug("Buscando solicitação por CPF: cpf={}, correlationId={}", cpf, correlationId);
        
        final SolicitacaoAberturaConta solicitacao = solicitacaoService.buscarPorCpf(cpf);
        return ResponseEntity.ok(toDTO(solicitacao));
    }

    @Operation(
            summary = "Verificar se existe conta para CPF",
            description = "Verifica se já existe uma conta ativa para o CPF informado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado da verificação",
                    content = @Content(schema = @Schema(implementation = Boolean.class))
            )
    })
    @GetMapping("/cpf/{cpf}/existe")
    public ResponseEntity<Boolean> verificarContaExiste(
            @Parameter(description = "CPF para verificação", required = true)
            @PathVariable final String cpf) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        log.debug("Verificando se existe conta para CPF: cpf={}, correlationId={}", cpf, correlationId);
        
        return ResponseEntity.ok(solicitacaoService.existeContaPorCpf(cpf));
    }

    private RespostaSolicitacaoDTO toDTO(final SolicitacaoAberturaConta solicitacao) {
        return new RespostaSolicitacaoDTO(
                solicitacao.getId(),
                solicitacao.getCpf(),
                solicitacao.getNome(),
                solicitacao.getStatus(),
                solicitacao.getNumeroConta(),
                solicitacao.getMotivoRejeicao(),
                solicitacao.getDataCriacao(),
                solicitacao.getDataAtualizacao()
        );
    }
}