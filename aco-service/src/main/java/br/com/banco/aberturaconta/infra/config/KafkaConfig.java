package br.com.banco.aberturaconta.infra.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    private final KafkaProperties kafkaProperties;
    
    public KafkaConfig(final KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        
        final Map<String, Object> configProps = new HashMap<>(kafkaProperties.buildProducerProperties(null));

        configProps.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, 
                KafkaCorrelationIdInterceptor.class.getName());
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}