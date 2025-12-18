package br.com.banco.aberturaconta.core.model;

import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoAberturaConta {
    
    private Long id;
    private String cpf;
    private String nome;
    private String email;
    private String telefone;
    private String canal; 
    private StatusSolicitacao status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private String numeroConta;
    private String motivoRejeicao;

    public void aprovar(final String numeroConta) {
        this.status = StatusSolicitacao.APROVADA;
        this.numeroConta = numeroConta;
    }

    public void rejeitar(final String motivo) {
        this.status = StatusSolicitacao.REJEITADA;
        this.motivoRejeicao = motivo;
    }

    public void atualizarStatus(final StatusSolicitacao novoStatus) {
        this.status = novoStatus;
    }
}