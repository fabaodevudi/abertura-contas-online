package br.com.banco.notification.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {
    
    public void enviar(String telefone, String mensagem) {
        log.info("=== SMS ENVIADO ===");
        log.info("Para: {}", telefone);
        log.info("Mensagem: {}", mensagem);
        log.info("==================");
    }
}