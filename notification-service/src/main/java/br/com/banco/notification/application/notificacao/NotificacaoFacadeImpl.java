package br.com.banco.notification.application.notificacao;

import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoFacadeImpl implements NotificacaoFacade {

    private final NotificacaoFactory notificacaoFactory;

    @Override
    public void notificarContaAberta(ContaAbertaEvent event) {
        log.info("Notificando conta aberta para canal: {}", event.getCanal());
        
        NotificacaoStrategy strategy = notificacaoFactory.getNotificadorPorCanal(event.getCanal());

        strategy.notificarPorEmailContaAberta(event);
        strategy.notificarPorSmsContaAberta(event);
        strategy.notificarPorPushContaAberta(event);
    }

    @Override
    @Async
    public void notificarRejeitada(SolicitacaoRejeitadaEvent event) {
        log.info("Notificando rejeição para canal: {}", event.getCanal());
        
        NotificacaoStrategy strategy = notificacaoFactory.getNotificadorPorCanal(event.getCanal());

        strategy.notificarPorEmailRejeitada(event);
        strategy.notificarPorSmsRejeitada(event);
        strategy.notificarPorPushRejeitada(event);
    }
}