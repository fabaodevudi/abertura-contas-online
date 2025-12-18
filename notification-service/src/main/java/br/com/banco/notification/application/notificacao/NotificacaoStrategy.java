package br.com.banco.notification.application.notificacao;

import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;

public interface NotificacaoStrategy {

    String getNomeCanal();

    void notificarPorEmailContaAberta(ContaAbertaEvent event);

    void notificarPorSmsContaAberta(ContaAbertaEvent event);

    void notificarPorPushContaAberta(ContaAbertaEvent event);

    void notificarPorEmailRejeitada(SolicitacaoRejeitadaEvent event);

    void notificarPorSmsRejeitada(SolicitacaoRejeitadaEvent event);

    void notificarPorPushRejeitada(SolicitacaoRejeitadaEvent event);
}