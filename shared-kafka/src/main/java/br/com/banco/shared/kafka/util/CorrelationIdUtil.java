package br.com.banco.shared.kafka.util;

import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationIdUtil {
    
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    
    private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();

    public static String getCorrelationId() {
        String correlationId = correlationIdHolder.get();
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }

    public static void setCorrelationId(final String correlationId) {
        if (correlationId != null && !correlationId.isEmpty()) {
            correlationIdHolder.set(correlationId);
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }

    public static void clearCorrelationId() {
        correlationIdHolder.remove();
        MDC.remove(CORRELATION_ID_KEY);
    }

    public static String getCorrelationIdHeader() {
        return CORRELATION_ID_HEADER;
    }

    public static boolean hasCorrelationId() {
        return correlationIdHolder.get() != null && !correlationIdHolder.get().isEmpty();
    }
}