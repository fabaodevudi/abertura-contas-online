package br.com.banco.aberturaconta.core.service;

import br.com.banco.aberturaconta.core.domain.Canal;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.aberturaconta.core.repository.SolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SolicitacaoServiceImpl (Domain Service).
 * 
 * Cobertura: 95%+ (objetivo)
 * Foco: Lógica de negócio pura, sem dependências de Spring/JPA.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitacaoServiceImpl - Domain Service Tests")
class SolicitacaoServiceImplTest {

    @Mock
    private SolicitacaoRepository repository;

    @InjectMocks
    private SolicitacaoServiceImpl solicitacaoService;

    private SolicitacaoAberturaConta solicitacao;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Deve buscar solicitação por ID com sucesso")
    void deveBuscarSolicitacaoPorIdComSucesso() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(solicitacao));

        // When
        final SolicitacaoAberturaConta resultado = solicitacaoService.buscarPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("12345678901", resultado.getCpf());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando solicitação não encontrada por ID")
    void deveLancarExcecaoQuandoSolicitacaoNaoEncontradaPorId() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitacaoService.buscarPorId(999L)
        );

        assertEquals("Solicitação não encontrada: 999", exception.getMessage());
        verify(repository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Deve buscar solicitação por CPF com sucesso")
    void deveBuscarSolicitacaoPorCpfComSucesso() {
        // Given
        when(repository.findByCpf("12345678901")).thenReturn(Optional.of(solicitacao));

        // When
        final SolicitacaoAberturaConta resultado = solicitacaoService.buscarPorCpf("12345678901");

        // Then
        assertNotNull(resultado);
        assertEquals("12345678901", resultado.getCpf());
        verify(repository, times(1)).findByCpf("12345678901");
    }

    @Test
    @DisplayName("Deve retornar null quando solicitação não encontrada por CPF")
    void deveRetornarNullQuandoSolicitacaoNaoEncontradaPorCpf() {
        // Given
        when(repository.findByCpf("99999999999")).thenReturn(Optional.empty());

        // When
        final SolicitacaoAberturaConta resultado = solicitacaoService.buscarPorCpf("99999999999");

        // Then
        assertNull(resultado);
        verify(repository, times(1)).findByCpf("99999999999");
    }

    @Test
    @DisplayName("Deve salvar solicitação com sucesso")
    void deveSalvarSolicitacaoComSucesso() {
        // Given
        when(repository.save(any(SolicitacaoAberturaConta.class))).thenReturn(solicitacao);

        // When
        final SolicitacaoAberturaConta resultado = solicitacaoService.salvar(solicitacao);

        // Then
        assertNotNull(resultado);
        assertEquals("12345678901", resultado.getCpf());
        verify(repository, times(1)).save(solicitacao);
    }

    @Test
    @DisplayName("Deve atualizar status da solicitação com sucesso")
    void deveAtualizarStatusComSucesso() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(solicitacao));
        when(repository.save(any(SolicitacaoAberturaConta.class))).thenReturn(solicitacao);

        // When
        solicitacaoService.atualizarStatus(1L, StatusSolicitacao.APROVADA);

        // Then
        assertEquals(StatusSolicitacao.APROVADA, solicitacao.getStatus());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(solicitacao);
    }

    @Test
    @DisplayName("Deve retornar true quando existe conta ativa para CPF")
    void deveRetornarTrueQuandoExisteContaAtiva() {
        // Given
        when(repository.existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        )).thenReturn(true);

        // When
        final boolean resultado = solicitacaoService.existeContaPorCpf("12345678901");

        // Then
        assertTrue(resultado);
        verify(repository, times(1)).existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        );
    }

    @Test
    @DisplayName("Deve retornar false quando não existe conta ativa para CPF")
    void deveRetornarFalseQuandoNaoExisteContaAtiva() {
        // Given
        when(repository.existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        )).thenReturn(false);

        // When
        final boolean resultado = solicitacaoService.existeContaPorCpf("12345678901");

        // Then
        assertFalse(resultado);
        verify(repository, times(1)).existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        );
    }

    @Test
    @DisplayName("Deve criar solicitação com sucesso quando não existe conta ativa")
    void deveCriarSolicitacaoComSucessoQuandoNaoExisteContaAtiva() {
        // Given
        when(repository.existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        )).thenReturn(false);
        when(repository.save(any(SolicitacaoAberturaConta.class))).thenReturn(solicitacao);

        // When
        final SolicitacaoAberturaConta resultado = solicitacaoService.criarSolicitacao(solicitacao);

        // Then
        assertNotNull(resultado);
        assertEquals("12345678901", resultado.getCpf());
        verify(repository, times(1)).existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        );
        verify(repository, times(1)).save(solicitacao);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando já existe conta ativa para CPF")
    void deveLancarExcecaoQuandoJaExisteContaAtiva() {
        // Given
        when(repository.existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        )).thenReturn(true);

        // When & Then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitacaoService.criarSolicitacao(solicitacao)
        );

        assertEquals("Já existe uma conta ativa para este CPF: 12345678901", exception.getMessage());
        verify(repository, times(1)).existsByCpfAndStatusIn(
                eq("12345678901"),
                eq(List.of(StatusSolicitacao.APROVADA, StatusSolicitacao.CONTA_ABERTA))
        );
        verify(repository, never()).save(any());
    }
}

