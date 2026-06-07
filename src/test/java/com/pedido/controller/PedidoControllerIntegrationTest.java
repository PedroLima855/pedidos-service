package com.pedido.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedido.dto.AtualizarStatusRequest;
import com.pedido.dto.CriarParceiroRequest;
import com.pedido.dto.CriarPedidoRequest;
import com.pedido.dto.ItemPedidoRequest;
import com.pedido.enums.StatusPedido;
import com.pedido.service.ParceiroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PedidoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParceiroService parceiroService;

    private ObjectMapper objectMapper;
    private Long parceiroId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setNome("Parceiro Teste");
        request.setLimiteCredito(new BigDecimal("10000.00"));
        parceiroId = parceiroService.criar(request).getId();
    }

    private CriarPedidoRequest criarPedidoRequest(BigDecimal preco, int quantidade) {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setParceiroId(parceiroId);

        ItemPedidoRequest item = new ItemPedidoRequest();
        item.setProduto("Produto Teste");
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(preco);
        request.setItens(List.of(item));

        return request;
    }

    @Nested
    @DisplayName("POST /api/pedidos")
    class CriarPedido {

        @Test
        @DisplayName("Deve criar pedido com sucesso - 201")
        void deveCriarPedido() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("1500.00"), 2);

            mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.parceiroId").value(parceiroId))
                    .andExpect(jsonPath("$.valorTotal").value(3000.00))
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(jsonPath("$.itens", hasSize(1)))
                    .andExpect(jsonPath("$.itens[0].produto").value("Produto Teste"));
        }

        @Test
        @DisplayName("Deve rejeitar pedido sem crédito - 422")
        void deveRejeitarSemCredito() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("20000.00"), 1);

            mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.erro").value(containsString("Crédito insuficiente")));
        }

        @Test
        @DisplayName("Deve rejeitar pedido sem itens - 400")
        void deveRejeitarSemItens() throws Exception {
            CriarPedidoRequest request = new CriarPedidoRequest();
            request.setParceiroId(parceiroId);
            request.setItens(List.of());

            mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve rejeitar pedido com parceiro inexistente - 404")
        void deveRejeitarParceiroInexistente() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("100.00"), 1);
            request.setParceiroId(999L);

            mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/pedidos")
    class BuscarPedido {

        @Test
        @DisplayName("Deve buscar pedido por ID - 200")
        void deveBuscarPorId() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("500.00"), 1);
            String response = mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            Long pedidoId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(get("/api/pedidos/{id}", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(pedidoId))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        @Test
        @DisplayName("Deve retornar 404 para pedido inexistente")
        void deveRetornar404() throws Exception {
            mockMvc.perform(get("/api/pedidos/{id}", 999))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve buscar pedidos por status - 200")
        void deveBuscarPorStatus() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("500.00"), 1);
            mockMvc.perform(post("/api/pedidos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(get("/api/pedidos/status/{status}", "PENDENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Deve buscar pedidos por período - 200")
        void deveBuscarPorPeriodo() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("500.00"), 1);
            mockMvc.perform(post("/api/pedidos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(get("/api/pedidos/periodo")
                            .param("inicio", "2020-01-01T00:00:00")
                            .param("fim", "2030-12-31T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("PATCH /api/pedidos/{id}/status")
    class AtualizarStatusPedido {

        @Test
        @DisplayName("Deve aprovar pedido e debitar crédito - 200")
        void deveAprovarPedido() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("1000.00"), 2);
            String response = mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            Long pedidoId = objectMapper.readTree(response).get("id").asLong();

            AtualizarStatusRequest statusRequest = new AtualizarStatusRequest();
            statusRequest.setStatus(StatusPedido.APROVADO);

            mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"));

            mockMvc.perform(get("/api/parceiros/{id}", parceiroId))
                    .andExpect(jsonPath("$.creditoDisponivel").value(8000.00));
        }

        @Test
        @DisplayName("Deve rejeitar transição inválida - 422")
        void deveRejeitarTransicaoInvalida() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("500.00"), 1);
            String response = mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            Long pedidoId = objectMapper.readTree(response).get("id").asLong();

            AtualizarStatusRequest aprovar = new AtualizarStatusRequest();
            aprovar.setStatus(StatusPedido.APROVADO);
            mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(aprovar)));

            AtualizarStatusRequest cancelar = new AtualizarStatusRequest();
            cancelar.setStatus(StatusPedido.CANCELADO);
            mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelar)));

            AtualizarStatusRequest reabrir = new AtualizarStatusRequest();
            reabrir.setStatus(StatusPedido.APROVADO);

            mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reabrir)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.erro").value(containsString("cancelado")));
        }
    }

    @Nested
    @DisplayName("POST /api/pedidos/{id}/cancelar")
    class CancelarPedido {

        @Test
        @DisplayName("Deve cancelar pedido aprovado e estornar crédito - 200")
        void deveCancelarAprovadoEEstornar() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("3000.00"), 1);
            String response = mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            Long pedidoId = objectMapper.readTree(response).get("id").asLong();

            AtualizarStatusRequest aprovar = new AtualizarStatusRequest();
            aprovar.setStatus(StatusPedido.APROVADO);
            mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(aprovar)));

            mockMvc.perform(post("/api/pedidos/{id}/cancelar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));

            mockMvc.perform(get("/api/parceiros/{id}", parceiroId))
                    .andExpect(jsonPath("$.creditoDisponivel").value(10000.00));
        }

        @Test
        @DisplayName("Deve cancelar pedido pendente sem estorno - 200")
        void deveCancelarPendenteSemEstorno() throws Exception {
            CriarPedidoRequest request = criarPedidoRequest(new BigDecimal("500.00"), 1);
            String response = mockMvc.perform(post("/api/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();

            Long pedidoId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(post("/api/pedidos/{id}/cancelar", pedidoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));

            mockMvc.perform(get("/api/parceiros/{id}", parceiroId))
                    .andExpect(jsonPath("$.creditoDisponivel").value(10000.00));
        }
    }
}
