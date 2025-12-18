package br.com.banco.aberturaconta.infra.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para solicitação de abertura de conta")
public record SolicitacaoAberturaContaDTO(
        @Schema(description = "CPF do solicitante (11 dígitos)", example = "12345678901", required = true)
        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
        String cpf,

        @Schema(description = "Nome completo do solicitante", example = "João Silva", required = true)
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @Schema(description = "Email do solicitante", example = "joao.silva@email.com", required = true)
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "Telefone do solicitante (10 ou 11 dígitos)", example = "11987654321", required = true)
        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 dígitos")
        String telefone,
        
        @Schema(description = "Canal de origem (FLAMENGO, AZUL, AMERICA)", example = "AMERICA")
        String canal
) {
}