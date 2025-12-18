package br.com.banco.aberturaconta.infra.config;

import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

@Slf4j
public class KafkaCorrelationIdInterceptor implements ProducerInterceptor<String, String> {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    
    @Override
    public ProducerRecord<String, String> onSend(final ProducerRecord<String, String> record) {
        
        final String correlationId = CorrelationIdUtil.getCorrelationId();

        record.headers().add(CORRELATION_ID_HEADER, correlationId.getBytes());
        
        log.debug("Correlation ID adicionado ao evento Kafka: topic={}, correlationId={}", 
                record.topic(), correlationId);
        
        return record;
    }
    
    @Override
    public void onAcknowledgement(final RecordMetadata metadata, final Exception exception) {
        
    }
    
    @Override
    public void close() {
        
    }
    
    @Override
    public void configure(final Map<String, ?> configs) {
        
    }
}