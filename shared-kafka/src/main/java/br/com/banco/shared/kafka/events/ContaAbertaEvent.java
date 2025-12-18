package br.com.banco.shared.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaAbertaEvent {
    private UUID eventoId;
    private Long solicitacaoId;
    private String cpf;
    private String nome;
    private String email;
    private String telefone;
    private String canal; 
    private String numeroConta;
    private LocalDateTime dataHora;
    private Map<String, Object> dadosAdicionais;
}