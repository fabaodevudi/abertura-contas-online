package br.com.banco.notification.application.notificacao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoRejeicaoEnum {

    TOPAZ("TOPAZ", "Problemas com dispositivo", 
          "Identificamos problemas relacionados ao seu dispositivo durante a análise de segurança."),

    ANTIFRAUDE("ANTIFRAUDE", "Análise antifraude", 
               "Sua solicitação não passou na análise antifraude."),

    PIX("PIX", "Análise PIX", 
        "Identificamos pendências relacionadas ao PIX durante a análise."),

    SERASA("SERASA", "Pendências no Serasa", 
           "Identificamos pendências no Serasa que precisam ser regularizadas antes de abrir sua conta."),

    PROVA_VIDA("PROVA_VIDA", "Análise de documentos e selfie", 
               "A análise de documentos e selfie não foi aprovada. Por favor, tente novamente com documentos válidos e selfie nítida."),

    OUTROS("OUTROS", "Análise não aprovada", 
           "Sua solicitação não foi aprovada na análise.");

    private final String codigo;
    private final String titulo;
    private final String mensagem;

    public static TipoRejeicaoEnum identificarPorMotivo(String motivoRejeicao) {
        if (motivoRejeicao == null || motivoRejeicao.isBlank()) {
            return OUTROS;
        }
        
        String motivoUpper = motivoRejeicao.toUpperCase();
        
        if (motivoUpper.contains("TOPAZ") || motivoUpper.contains("DISPOSITIVO") || motivoUpper.contains("DEVICE")) {
            return TOPAZ;
        }
        
        if (motivoUpper.contains("ANTIFRAUDE") || motivoUpper.contains("FRAUDE")) {
            return ANTIFRAUDE;
        }
        
        if (motivoUpper.contains("PIX")) {
            return PIX;
        }
        
        if (motivoUpper.contains("SERASA") || motivoUpper.contains("SCORE") || motivoUpper.contains("PENDENCIA")) {
            return SERASA;
        }
        
        if (motivoUpper.contains("PROVA_VIDA") || motivoUpper.contains("PROVA DE VIDA") 
            || motivoUpper.contains("SELFIE") || motivoUpper.contains("DOCUMENTO") 
            || motivoUpper.contains("BIOMETRIA") || motivoUpper.contains("SIMILARIDADE")) {
            return PROVA_VIDA;
        }
        
        return OUTROS;
    }

    public boolean isRejeicaoConhecida() {
        return this != OUTROS;
    }
}