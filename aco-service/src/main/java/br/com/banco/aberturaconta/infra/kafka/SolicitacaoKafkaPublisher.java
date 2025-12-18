package br.com.banco.aberturaconta.infra.kafka;

import br.com.banco.aberturaconta.core.domain.Canal;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.shared.kafka.config.KafkaTopics;
import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publicarContaAberta(final SolicitacaoAberturaConta solicitacao) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        
        try {
            final String canal = obterCanal(solicitacao.getCanal());
            
            final ContaAbertaEvent event = ContaAbertaEvent.builder()
                .eventoId(UUID.randomUUID())
                .solicitacaoId(solicitacao.getId())
                .cpf(solicitacao.getCpf())
                .nome(solicitacao.getNome())
                .email(solicitacao.getEmail())
                .telefone(solicitacao.getTelefone())
                .canal(canal)
                .numeroConta(solicitacao.getNumeroConta())
                .dataHora(LocalDateTime.now())
                .build();

            final String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                KafkaTopics.CONTA_ABERTA,
                solicitacao.getId().toString(),
                eventJson
            );

            log.info("Evento ContaAberta publicado no Kafka: solicitacaoId={}, canal={}, correlationId={}", 
                    solicitacao.getId(), canal, correlationId);
        } catch (Exception e) {
            log.error("Erro ao publicar evento ContaAberta no Kafka: solicitacaoId={}, correlationId={}", 
                    solicitacao.getId(), correlationId, e);
            
        }
    }

    @Async
    public void publicarSolicitacaoRejeitada(final SolicitacaoAberturaConta solicitacao) {
        final String correlationId = CorrelationIdUtil.getCorrelationId();
        
        try {
            final String canal = obterCanal(solicitacao.getCanal());
            final String tipoRejeicao = identificarTipoRejeicao(solicitacao.getMotivoRejeicao());
            
            final SolicitacaoRejeitadaEvent event = SolicitacaoRejeitadaEvent.builder()
                .eventoId(UUID.randomUUID())
                .solicitacaoId(solicitacao.getId())
                .cpf(solicitacao.getCpf())
                .nome(solicitacao.getNome())
                .email(solicitacao.getEmail())
                .telefone(solicitacao.getTelefone())
                .canal(canal)
                .motivoRejeicao(solicitacao.getMotivoRejeicao())
                .tipoRejeicao(tipoRejeicao)
                .dataHora(LocalDateTime.now())
                .build();

            final String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                KafkaTopics.SOLICITACAO_REJEITADA,
                solicitacao.getId().toString(),
                eventJson
            );

            log.info("Evento SolicitacaoRejeitada publicado no Kafka: solicitacaoId={}, canal={}, correlationId={}", 
                    solicitacao.getId(), canal, correlationId);
        } catch (Exception e) {
            log.error("Erro ao publicar evento SolicitacaoRejeitada no Kafka: solicitacaoId={}, correlationId={}", 
                    solicitacao.getId(), correlationId, e);
            
        }
    }

    private String obterCanal(final String canal) {
        return Canal.fromString(canal).name();
    }

    private String identificarTipoRejeicao(final String motivoRejeicao) {
        if (motivoRejeicao == null || motivoRejeicao.isBlank()) {
            return "OUTROS";
        }
        
        final String motivoUpper = motivoRejeicao.toUpperCase();
        
        if (motivoUpper.contains("TOPAZ") || motivoUpper.contains("DISPOSITIVO") || motivoUpper.contains("DEVICE")) {
            return "TOPAZ";
        }
        
        if (motivoUpper.contains("ANTIFRAUDE") || motivoUpper.contains("FRAUDE")) {
            return "ANTIFRAUDE";
        }
        
        if (motivoUpper.contains("PIX")) {
            return "PIX";
        }
        
        if (motivoUpper.contains("SERASA") || motivoUpper.contains("SCORE") || motivoUpper.contains("PENDENCIA")) {
            return "SERASA";
        }
        
        if (motivoUpper.contains("PROVA_VIDA") || motivoUpper.contains("PROVA DE VIDA") 
            || motivoUpper.contains("SELFIE") || motivoUpper.contains("DOCUMENTO") 
            || motivoUpper.contains("BIOMETRIA") || motivoUpper.contains("SIMILARIDADE")) {
            return "PROVA_VIDA";
        }
        
        return "OUTROS";
    }
}