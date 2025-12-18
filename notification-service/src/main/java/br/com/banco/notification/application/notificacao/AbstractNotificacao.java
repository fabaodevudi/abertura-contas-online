package br.com.banco.notification.application.notificacao;

import br.com.banco.notification.core.service.EmailService;
import br.com.banco.notification.core.service.PushService;
import br.com.banco.notification.core.service.SmsService;
import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractNotificacao implements NotificacaoStrategy {

    protected final EmailService emailService;
    protected final SmsService smsService;
    protected final PushService pushService;

    @Override
    public void notificarPorEmailContaAberta(ContaAbertaEvent event) {
        log.info("Enviando email de conta aberta para canal: {}", getNomeCanal());
        String assunto = getAssuntoEmailContaAberta();
        String corpo = getCorpoEmailContaAberta(event);
        emailService.enviar(event.getEmail(), assunto, corpo);
    }

    @Override
    public void notificarPorEmailRejeitada(SolicitacaoRejeitadaEvent event) {
        log.info("Enviando email de rejeição para canal: {}", getNomeCanal());
        TipoRejeicaoEnum tipoRejeicao = identificarTipoRejeicao(event);
        String assunto = getAssuntoEmailRejeitada(tipoRejeicao);
        String corpo = getCorpoEmailRejeitada(event, tipoRejeicao);
        emailService.enviar(event.getEmail(), assunto, corpo);
    }

    @Override
    public void notificarPorSmsContaAberta(ContaAbertaEvent event) {
        log.info("Enviando SMS de conta aberta para canal: {}", getNomeCanal());
        String mensagem = getMensagemSmsContaAberta(event);
        smsService.enviar(event.getTelefone(), mensagem);
    }

    @Override
    public void notificarPorSmsRejeitada(SolicitacaoRejeitadaEvent event) {
        log.info("Enviando SMS de rejeição para canal: {}", getNomeCanal());
        TipoRejeicaoEnum tipoRejeicao = identificarTipoRejeicao(event);
        String mensagem = getMensagemSmsRejeitada(event, tipoRejeicao);
        smsService.enviar(event.getTelefone(), mensagem);
    }

    @Override
    public void notificarPorPushContaAberta(ContaAbertaEvent event) {
        log.info("Enviando Push de conta aberta para canal: {}", getNomeCanal());
        String titulo = getTituloPushContaAberta();
        String corpo = getCorpoPushContaAberta(event);
        pushService.enviar(event.getEmail(), titulo, corpo);
    }

    @Override
    public void notificarPorPushRejeitada(SolicitacaoRejeitadaEvent event) {
        log.info("Enviando Push de rejeição para canal: {}", getNomeCanal());
        TipoRejeicaoEnum tipoRejeicao = identificarTipoRejeicao(event);
        String titulo = getTituloPushRejeitada(tipoRejeicao);
        String corpo = getCorpoPushRejeitada(event, tipoRejeicao);
        pushService.enviar(event.getEmail(), titulo, corpo);
    }

    protected abstract String getAssuntoEmailContaAberta();
    protected abstract String getCorpoEmailContaAberta(ContaAbertaEvent event);
    protected abstract String getMensagemSmsContaAberta(ContaAbertaEvent event);
    protected abstract String getTituloPushContaAberta();
    protected abstract String getCorpoPushContaAberta(ContaAbertaEvent event);

    protected abstract String getAssuntoEmailRejeitada(TipoRejeicaoEnum tipoRejeicao);
    protected abstract String getCorpoEmailRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao);
    protected abstract String getMensagemSmsRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao);
    protected abstract String getTituloPushRejeitada(TipoRejeicaoEnum tipoRejeicao);
    protected abstract String getCorpoPushRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao);

    protected TipoRejeicaoEnum identificarTipoRejeicao(SolicitacaoRejeitadaEvent event) {
        if (event.getTipoRejeicao() != null && !event.getTipoRejeicao().isBlank()) {
            try {
                return TipoRejeicaoEnum.valueOf(event.getTipoRejeicao());
            } catch (IllegalArgumentException e) {
                log.warn("Tipo de rejeição inválido: {}. Identificando pelo motivo.", event.getTipoRejeicao());
            }
        }
        return TipoRejeicaoEnum.identificarPorMotivo(event.getMotivoRejeicao());
    }
}