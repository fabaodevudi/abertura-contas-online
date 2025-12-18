package br.com.banco.aberturaconta.infra.config;

import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(1) 
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        
        try {
            
            String correlationId = request.getHeader(CorrelationIdUtil.getCorrelationIdHeader());

            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = CorrelationIdUtil.getCorrelationId();
                log.debug("Correlation ID gerado: {}", correlationId);
            } else {
                CorrelationIdUtil.setCorrelationId(correlationId);
                log.debug("Correlation ID recebido: {}", correlationId);
            }

            response.setHeader(CorrelationIdUtil.getCorrelationIdHeader(), correlationId);

            filterChain.doFilter(request, response);
            
        } finally {
            
            CorrelationIdUtil.clearCorrelationId();
        }
    }
}