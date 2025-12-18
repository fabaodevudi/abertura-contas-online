package br.com.banco.notification.application.notificacao;

import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;

public interface NotificacaoFacade {

    void notificarContaAberta(ContaAbertaEvent event);

    void notificarRejeitada(SolicitacaoRejeitadaEvent event);
}