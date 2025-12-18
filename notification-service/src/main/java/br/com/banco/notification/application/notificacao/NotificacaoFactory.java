package br.com.banco.notification.application.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificacaoFactory {

    private final Map<String, NotificacaoStrategy> strategies;

    @Autowired
    public NotificacaoFactory(java.util.List<NotificacaoStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                NotificacaoStrategy::getNomeCanal,
                Function.identity()
            ));
    }

    public NotificacaoStrategy getNotificadorPorCanal(String canal) {
        NotificacaoStrategy strategy = strategies.get(canal.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                canal + " - Canal não possui notificação implementada."
            );
        }
        return strategy;
    }
}