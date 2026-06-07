package com.pedido.service;

import com.pedido.dto.CriarParceiroRequest;
import com.pedido.entity.Parceiro;
import com.pedido.repository.ParceiroRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParceiroServiceTest {

    @Mock
    private ParceiroRepository parceiroRepository;

    @InjectMocks
    private ParceiroService parceiroService;

    @Test
    @DisplayName("Deve criar parceiro com crédito disponível igual ao limite")
    void deveCriarParceiro() {
        CriarParceiroRequest request = new CriarParceiroRequest();
        request.setNome("Parceiro Teste");
        request.setLimiteCredito(new BigDecimal("10000.00"));

        when(parceiroRepository.save(any(Parceiro.class))).thenAnswer(inv -> {
            Parceiro p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Parceiro result = parceiroService.criar(request);

        assertThat(result.getNome()).isEqualTo("Parceiro Teste");
        assertThat(result.getLimiteCreditoTotal()).isEqualByComparingTo("10000.00");
        assertThat(result.getCreditoDisponivel()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("Deve buscar parceiro por ID")
    void deveBuscarPorId() {
        Parceiro parceiro = Parceiro.builder().id(1L).nome("Teste").build();
        when(parceiroRepository.findById(1L)).thenReturn(Optional.of(parceiro));

        Parceiro result = parceiroService.buscarPorId(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando parceiro não encontrado")
    void deveLancarExcecaoParceiroNaoEncontrado() {
        when(parceiroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parceiroService.buscarPorId(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Parceiro não encontrado");
    }

    @Test
    @DisplayName("Deve listar todos os parceiros")
    void deveListarTodos() {
        Parceiro p1 = Parceiro.builder().id(1L).nome("A").build();
        Parceiro p2 = Parceiro.builder().id(2L).nome("B").build();
        when(parceiroRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Parceiro> result = parceiroService.listarTodos();

        assertThat(result).hasSize(2);
    }
}
