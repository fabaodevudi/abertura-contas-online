package br.com.banco.aberturaconta.infra.rest.controller;

import br.com.banco.aberturaconta.application.service.SolicitacaoApplicationService;
import br.com.banco.aberturaconta.core.domain.StatusSolicitacao;
import br.com.banco.aberturaconta.core.model.SolicitacaoAberturaConta;
import br.com.banco.shared.kafka.util.CorrelationIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para SolicitacaoAberturaContaController.
 * 
 * Foco: Validação de endpoints REST, tratamento de exceções HTTP.
 */
@WebMvcTest(SolicitacaoAberturaContaController.class)
@DisplayName("SolicitacaoAberturaContaController - Integration Tests")
class SolicitacaoAberturaContaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolicitacaoApplicationService solicitacaoService;

    @Autowired
    private ObjectMapper objectMapper;

    private SolicitacaoAberturaConta solicitacao;

    @BeforeEach
    void setUp() {
        CorrelationIdUtil.setCorrelationId("test-correlation-id");

        solicitacao = SolicitacaoAberturaConta.builder()
                .id(1L)
                .cpf("12345678901")
                .nome("João Silva")
                .email("joao.silva@email.com")
                .telefone("11987654321")
                .canal("AMERICA")
                .status(StatusSolicitacao.INICIADA)
                .build();
    }

    @Test
    @DisplayName("Deve criar solicitação com sucesso - 201 Created")
    void deveCriarSolicitacaoComSucesso() throws Exception {
        // Given
        final String requestBody = """
                {
                    "cpf": "12345678901",
                    "nome": "João Silva",
                    "email": "joao.silva@email.com",
                    "telefone": "11987654321",
                    "canal": "AMERICA"
                }
                """;

        when(solicitacaoService.criarSolicitacao(any())).thenReturn(solicitacao);

        // When & Then
        mockMvc.perform(post("/api/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-Correlation-Id", "test-correlation-id"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.status").value("INICIADA"));
    }

    @Test
    @DisplayName("Deve retornar 400 quando DTO inválido")
    void deveRetornar400QuandoDTOInvalido() throws Exception {
        // Given
        final String requestBody = """
                {
                    "cpf": "123",
                    "nome": "",
                    "email": "email-invalido"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}

