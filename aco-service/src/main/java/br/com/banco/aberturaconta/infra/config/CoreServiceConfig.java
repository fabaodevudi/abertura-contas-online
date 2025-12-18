package br.com.banco.aberturaconta.infra.config;

import br.com.banco.aberturaconta.core.repository.SolicitacaoRepository;
import br.com.banco.aberturaconta.core.service.ISolicitacaoService;
import br.com.banco.aberturaconta.core.service.SolicitacaoServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceConfig {
    
    @Bean
    public ISolicitacaoService solicitacaoService(SolicitacaoRepository repository) {
        return new SolicitacaoServiceImpl(repository);
    }
}