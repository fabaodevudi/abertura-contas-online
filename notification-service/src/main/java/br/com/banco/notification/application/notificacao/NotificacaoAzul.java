package br.com.banco.notification.application.notificacao;

import br.com.banco.notification.core.service.EmailService;
import br.com.banco.notification.core.service.PushService;
import br.com.banco.notification.core.service.SmsService;
import br.com.banco.shared.kafka.events.ContaAbertaEvent;
import br.com.banco.shared.kafka.events.SolicitacaoRejeitadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificacaoAzul extends AbstractNotificacao {

    public NotificacaoAzul(EmailService emailService, SmsService smsService, PushService pushService) {
        super(emailService, smsService, pushService);
    }

    @Override
    public String getNomeCanal() {
        return "AZUL";
    }

    @Override
    protected String getAssuntoEmailContaAberta() {
        return "Bem-vindo à Azul! ✈️ Sua conta está aberta";
    }

    @Override
    protected String getCorpoEmailContaAberta(ContaAbertaEvent event) {
        return String.format(
            "<html><body style='background-color: #0066CC; color: white; padding: 20px;'>" +
            "<h1>✈️ Olá!</h1>" +
            "<p>Sua conta Azul foi aberta com sucesso!</p>" +
            "<p><strong>Número da conta:</strong> %s</p>" +
            "<p>Acesse o app e comece a acumular milhas!</p>" +
            "</body></html>",
            event.getNumeroConta()
        );
    }

    @Override
    protected String getMensagemSmsContaAberta(ContaAbertaEvent event) {
        return String.format(
            "Olá! Sua conta Azul foi aberta com sucesso! ✈️ Conta: %s. Acesse o app e comece a acumular milhas!",
            event.getNumeroConta()
        );
    }

    @Override
    protected String getTituloPushContaAberta() {
        return "Conta aberta! ✈️";
    }

    @Override
    protected String getCorpoPushContaAberta(ContaAbertaEvent event) {
        return "Suas milhas já estão disponíveis no app Azul!";
    }

    @Override
    protected String getAssuntoEmailRejeitada(TipoRejeicaoEnum tipoRejeicao) {
        return String.format("Solicitação de conta - Azul - %s", tipoRejeicao.getTitulo());
    }

    @Override
    protected String getCorpoEmailRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao) {
        String mensagemEspecifica = obterMensagemEspecificaRejeicao(tipoRejeicao);
        return String.format(
            "<html><body style='background-color: #0066CC; color: white; padding: 20px;'>" +
            "<h1>✈️ Olá!</h1>" +
            "<p>Sua solicitação de conta Azul não foi aprovada.</p>" +
            "<p><strong>%s</strong></p>" +
            "<p>%s</p>" +
            "<p>Entre em contato conosco para mais informações ou tente novamente.</p>" +
            "</body></html>",
            tipoRejeicao.getTitulo(),
            mensagemEspecifica
        );
    }

    @Override
    protected String getMensagemSmsRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao) {
        String mensagemEspecifica = obterMensagemEspecificaRejeicao(tipoRejeicao);
        return String.format(
            "Olá! ✈️ %s %s Entre em contato conosco.",
            tipoRejeicao.getTitulo() + ".",
            mensagemEspecifica
        );
    }

    @Override
    protected String getTituloPushRejeitada(TipoRejeicaoEnum tipoRejeicao) {
        return String.format("Solicitação - %s", tipoRejeicao.getTitulo());
    }

    @Override
    protected String getCorpoPushRejeitada(SolicitacaoRejeitadaEvent event, TipoRejeicaoEnum tipoRejeicao) {
        String mensagemEspecifica = obterMensagemEspecificaRejeicao(tipoRejeicao);
        return String.format("%s Acesse o app Azul para mais informações.", mensagemEspecifica);
    }

    private String obterMensagemEspecificaRejeicao(TipoRejeicaoEnum tipoRejeicao) {
        return switch (tipoRejeicao) {
            case PROVA_VIDA -> "A análise de documentos e selfie não passou. Tente novamente com documentos válidos e selfie nítida.";
            case TOPAZ -> "Identificamos problemas relacionados ao seu dispositivo durante a análise de segurança.";
            case SERASA -> "Identificamos pendências no Serasa que precisam ser regularizadas antes de abrir sua conta.";
            case ANTIFRAUDE -> "Sua solicitação não passou na análise antifraude.";
            case PIX -> "Identificamos pendências relacionadas ao PIX durante a análise.";
            case OUTROS -> tipoRejeicao.getMensagem();
        };
    }
}