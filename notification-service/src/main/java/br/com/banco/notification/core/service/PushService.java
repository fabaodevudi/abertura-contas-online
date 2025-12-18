package br.com.banco.notification.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushService {
    
    public void enviar(String deviceToken, String titulo, String corpo) {
        log.info("=== PUSH NOTIFICATION ENVIADO ===");
        log.info("Device Token: {}", deviceToken);
        log.info("Título: {}", titulo);
        log.info("Corpo: {}", corpo);
        log.info("=================================");
    }
}