package br.com.banco.aberturaconta.application.service;

import br.com.banco.aberturaconta.core.domain.Canal;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.service.ISolicitacaoService;
import br.com.banco.aberturaconta.infra.dto.SolicitacaoAberturaContaDTO;
import br.com.banco.aberturaconta.infra.event.SolicitacaoCriadaEvent;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SolicitacaoApplicationService (Application Layer).
 * 
 * Foco: Orquestração, conversão DTO → Model, tratamento de exceções.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitacaoApplicationService - Application Service Tests")
class SolicitacaoApplicationServiceTest {

    @Mock
    private ISolicitacaoService solicitacaoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SolicitacaoApplicationService applicationService;

    private SolicitacaoAberturaContaDTO dto;
    private SolicitacaoAberturaConta solicitacao;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.setCorrelationId("test-correlation-id");

        dto = new SolicitacaoAberturaContaDTO(
                "12345678901",
                "João Silva",
                "joao.silva@email.com",
                "11987654321",
                "AMERICA"
        );

        solicitacao = SolicitacaoAberturaConta.builder()
                .id(1L)
                .cpf("12345678901")
                .nome("João Silva")
                .email("joao.silva@email.com")
                .telefone("11987654321")
                .canal(Canal.AMERICA.name())
                .status(StatusSolicitacao.INICIADA)
                .build();
    }

    @Test
    @DisplayName("Deve criar solicitação a partir de DTO com sucesso")
    void deveCriarSolicitacaoAPartirDeDTOComSucesso() {
        // Given
        when(solicitacaoService.criarSolicitacao(any(SolicitacaoAberturaConta.class)))
                .thenReturn(solicitacao);

        // When
        final SolicitacaoAberturaConta resultado = applicationService.criarSolicitacao(dto);

        // Then
        assertNotNull(resultado);
        assertEquals("12345678901", resultado.getCpf());
        assertEquals(StatusSolicitacao.INICIADA, resultado.getStatus());
        assertEquals(Canal.AMERICA.name(), resultado.getCanal());

        // Verifica que evento foi publicado
        final ArgumentCaptor<SolicitacaoCriadaEvent> eventCaptor = 
                ArgumentCaptor.forClass(SolicitacaoCriadaEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        final SolicitacaoCriadaEvent event = eventCaptor.getValue();
        assertEquals(1L, event.getSolicitacaoId());
    }

    @Test
    @DisplayName("Deve usar canal default quando DTO não informa canal")
    void deveUsarCanalDefaultQuandoDTONaoInformaCanal() {
        // Given
        final SolicitacaoAberturaContaDTO dtoSemCanal = new SolicitacaoAberturaContaDTO(
                "12345678901",
                "João Silva",
                "joao.silva@email.com",
                "11987654321",
                null
        );

        when(solicitacaoService.criarSolicitacao(any(SolicitacaoAberturaConta.class)))
                .thenReturn(solicitacao);

        // When
        applicationService.criarSolicitacao(dtoSemCanal);

        // Then
        final ArgumentCaptor<SolicitacaoAberturaConta> modelCaptor = 
                ArgumentCaptor.forClass(SolicitacaoAberturaConta.class);
        verify(solicitacaoService, times(1)).criarSolicitacao(modelCaptor.capture());

        final SolicitacaoAberturaConta model = modelCaptor.getValue();
        assertEquals(Canal.getDefault().name(), model.getCanal());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 quando já existe conta ativa")
    void deveLancarResponseStatusException409QuandoJaExisteContaAtiva() {
        // Given
        when(solicitacaoService.criarSolicitacao(any(SolicitacaoAberturaConta.class)))
                .thenThrow(new IllegalArgumentException("Já existe uma conta ativa para este CPF: 12345678901"));

        // When & Then
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.criarSolicitacao(dto)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Já existe uma conta ativa"));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 400 para IllegalArgumentException genérico")
    void deveLancarResponseStatusException400ParaIllegalArgumentExceptionGenerico() {
        // Given
        when(solicitacaoService.criarSolicitacao(any(SolicitacaoAberturaConta.class)))
                .thenThrow(new IllegalArgumentException("Erro de validação"));

        // When & Then
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.criarSolicitacao(dto)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 quando solicitação não encontrada por ID")
    void deveLancarResponseStatusException404QuandoSolicitacaoNaoEncontradaPorId() {
        // Given
        when(solicitacaoService.buscarPorId(999L))
                .thenThrow(new IllegalArgumentException("Solicitação não encontrada: 999"));

        // When & Then
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.buscarPorId(999L)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 quando solicitação não encontrada por CPF")
    void deveLancarResponseStatusException404QuandoSolicitacaoNaoEncontradaPorCpf() {
        // Given
        when(solicitacaoService.buscarPorCpf("99999999999")).thenReturn(null);

        // When & Then
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.buscarPorCpf("99999999999")
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Solicitação não encontrada"));
    }
}

