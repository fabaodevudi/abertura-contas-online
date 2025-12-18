package br.com.banco.notification.application;

import br.com.banco.notification.application.notificacao.NotificacaoFacade;
import br.com.banco.shared.kafka.config.KafkaTopics;
import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoStatusFinalConsumer {
    
    private final NotificacaoFacade notificacaoFacade;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = KafkaTopics.CONTA_ABERTA,
        groupId = "notification-service"
    )
    public void processarContaAberta(ConsumerRecord<String, String> record) {
        String correlationId = extractCorrelationId(record);
        CorrelationIdUtil.setCorrelationId(correlationId);
        
        try {
            log.info("Recebendo evento ContaAberta: correlationId={}, message={}", 
                    correlationId, record.value());
            
            ContaAbertaEvent event = objectMapper.readValue(record.value(), ContaAbertaEvent.class);
            notificacaoFacade.notificarContaAberta(event);
            
        } catch (Exception e) {
            log.error("Erro ao processar evento ContaAberta: correlationId={}", correlationId, e);
            throw new RuntimeException("Erro ao processar evento", e);
        } finally {
            CorrelationIdUtil.clearCorrelationId();
        }
    }
    
    @KafkaListener(
        topics = KafkaTopics.SOLICITACAO_REJEITADA,
        groupId = "notification-service"
    )
    public void processarSolicitacaoRejeitada(ConsumerRecord<String, String> record) {
        String correlationId = extractCorrelationId(record);
        CorrelationIdUtil.setCorrelationId(correlationId);
        
        try {
            log.info("Recebendo evento SolicitacaoRejeitada: correlationId={}, message={}", 
                    correlationId, record.value());
            
            SolicitacaoRejeitadaEvent event = objectMapper.readValue(
                record.value(), 
                SolicitacaoRejeitadaEvent.class
            );
            notificacaoFacade.notificarRejeitada(event);
            
        } catch (Exception e) {
            log.error("Erro ao processar evento SolicitacaoRejeitada: correlationId={}", correlationId, e);
            throw new RuntimeException("Erro ao processar evento", e);
        } finally {
            CorrelationIdUtil.clearCorrelationId();
        }
    }
    
    private String extractCorrelationId(ConsumerRecord<String, String> record) {
        String correlationIdHeader = CorrelationIdUtil.getCorrelationIdHeader();
        
        if (record.headers() != null) {
            var headers = record.headers().headers(correlationIdHeader);
            if (headers != null) {
                var iterator = headers.iterator();
                if (iterator.hasNext()) {
                    byte[] correlationIdBytes = iterator.next().value();
                    if (correlationIdBytes != null) {
                        return new String(correlationIdBytes);
                    }
                }
            }
        }
        
        return CorrelationIdUtil.getCorrelationId();
    }
}