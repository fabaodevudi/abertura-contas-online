package br.com.banco.aberturaconta.core.domain;

public enum StatusSolicitacao {
    INICIADA("Iniciada"),
    VALIDANDO_TOPAZ("Validando Topaz"),
    VALIDANDO_ANTIFRAUDE("Validando Antifraude"),
    VALIDANDO_PIX("Validando PIX"),
    VALIDANDO_SERASA("Validando Serasa"),
    VALIDANDO_PROVA_VIDA("Validando Prova de Vida"),
    AGUARDANDO_SISTEMA_INTERNO("Aguardando Sistema Interno"),
    APROVADA("Aprovada"),
    REJEITADA("Rejeitada"),
    CONTA_ABERTA("Conta Aberta");

    private final String descricao;

    StatusSolicitacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}