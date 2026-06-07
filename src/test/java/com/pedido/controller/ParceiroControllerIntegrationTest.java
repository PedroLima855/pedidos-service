package com.pedido.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedido.dto.CriarParceiroRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ParceiroControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Deve criar parceiro - 201")
    void deveCriarParceiro() throws Exception {
        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setNome("Parceiro Novo");
        request.setLimiteCredito(new BigDecimal("15000.00"));

        mockMvc.perform(post("/api/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Parceiro Novo"))
                .andExpect(jsonPath("$.limiteCreditoTotal").value(15000.00))
                .andExpect(jsonPath("$.creditoDisponivel").value(15000.00));
    }

    @Test
    @DisplayName("Deve rejeitar parceiro sem nome - 400")
    void deveRejeitarSemNome() throws Exception {
        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setLimiteCredito(new BigDecimal("5000.00"));

        mockMvc.perform(post("/api/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve buscar parceiro por ID - 200")
    void deveBuscarPorId() throws Exception {
        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setNome("Busca Teste");
        request.setLimiteCredito(new BigDecimal("8000.00"));

        String response = mockMvc.perform(post("/api/parceiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/parceiros/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Busca Teste"));
    }

    @Test
    @DisplayName("Deve retornar 404 para parceiro inexistente")
    void deveRetornar404() throws Exception {
        mockMvc.perform(get("/api/parceiros/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve listar todos os parceiros - 200")
    void deveListarTodos() throws Exception {
        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setNome("Parceiro Lista");
        request.setLimiteCredito(new BigDecimal("5000.00"));

        mockMvc.perform(post("/api/parceiros")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/parceiros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
