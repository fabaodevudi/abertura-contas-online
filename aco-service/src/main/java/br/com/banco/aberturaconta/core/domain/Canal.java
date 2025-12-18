package br.com.banco.aberturaconta.core.domain;

public enum Canal {
    FLAMENGO("Flamengo"),
    AZUL("Azul"),
    AMERICA("America");
    
    private final String descricao;
    
    Canal(final String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }

    public static Canal getDefault() {
        return AMERICA;
    }

    public static Canal fromString(final String canal) {
        if (canal == null || canal.isBlank()) {
            return getDefault();
        }
        
        try {
            return valueOf(canal.toUpperCase());
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
}