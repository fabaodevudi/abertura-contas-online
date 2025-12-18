package br.com.banco.aberturaconta.infra.dto;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta com os dados da solicitação de abertura de conta")
public record RespostaSolicitacaoDTO(
        @Schema(description = "ID da solicitação", example = "1")
        Long id,
        
        @Schema(description = "CPF do solicitante", example = "12345678901")
        String cpf,
        
        @Schema(description = "Nome do solicitante", example = "João Silva")
        String nome,
        
        @Schema(description = "Status da solicitação", example = "INICIADA")
        StatusSolicitacao status,
        
        @Schema(description = "Número da conta (gerado após aprovação)", example = "12345-6")
        String numeroConta,
        
        @Schema(description = "Motivo da rejeição (se aplicável)", example = "Score Serasa abaixo do mínimo")
        String motivoRejeicao,
        
        @Schema(description = "Data de criação da solicitação")
        LocalDateTime dataCriacao,
        
        @Schema(description = "Data da última atualização")
        LocalDateTime dataAtualizacao
) {
    public static RespostaSolicitacaoDTO of(Long id, String cpf, String nome, StatusSolicitacao status,
                                             String numeroConta, String motivoRejeicao,
                                             LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        return new RespostaSolicitacaoDTO(id, cpf, nome, status, numeroConta, motivoRejeicao, dataCriacao, dataAtualizacao);
    }
}