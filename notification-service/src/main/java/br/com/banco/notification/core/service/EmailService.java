package br.com.banco.notification.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
    
    public void enviar(String destinatario, String assunto, String corpo) {
        log.info("=== EMAIL ENVIADO ===");
        log.info("Para: {}", destinatario);
        log.info("Assunto: {}", assunto);
        log.info("Corpo: {}", corpo);
        log.info("====================");
    }
}