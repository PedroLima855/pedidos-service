package com.pedido.service;

import com.pedido.dto.CriarPedidoRequest;
import com.pedido.dto.ItemPedidoRequest;
import com.pedido.dto.PedidoResponse;
import com.pedido.entity.ItemPedido;
import com.pedido.entity.Parceiro;
import com.pedido.entity.Pedido;
import com.pedido.enums.StatusPedido;
import com.pedido.exception.NegocioException;
import com.pedido.repository.ParceiroRepository;
import com.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ParceiroRepository parceiroRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PedidoService pedidoService;

    private Parceiro parceiro;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        parceiro = Parceiro.builder()
                .id(1L)
                .nome("Parceiro Teste")
                .limiteCreditoTotal(new BigDecimal("10000.00"))
                .creditoDisponivel(new BigDecimal("10000.00"))
                .criadoEm(LocalDateTime.now())
                .build();

        pedido = Pedido.builder()
                .id(1L)
                .parceiro(parceiro)
                .valorTotal(new BigDecimal("7750.00"))
                .status(StatusPedido.PENDENTE)
                .itens(new ArrayList<>())
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Criar Pedido")
    class CriarPedido {

        @Test
        @DisplayName("Deve criar pedido com sucesso")
        void deveCriarPedidoComSucesso() {
            CriarPedidoRequest request = new CriarPedidoRequest();
            request.setParceiroId(1L);

            ItemPedidoRequest item = new ItemPedidoRequest();
            item.setProduto("Notebook");
            item.setQuantidade(2);
            item.setPrecoUnitario(new BigDecimal("3500.00"));
            request.setItens(List.of(item));

            when(parceiroRepository.findByIdComLock(1L)).thenReturn(Optional.of(parceiro));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
                Pedido p = inv.getArgument(0);
                p.setId(1L);
                p.setStatus(StatusPedido.PENDENTE);
                p.setCriadoEm(LocalDateTime.now());
                p.setAtualizadoEm(LocalDateTime.now());
                return p;
            });

            PedidoResponse response = pedidoService.criar(request);

            assertThat(response).isNotNull();
            assertThat(response.getValorTotal()).isEqualByComparingTo("7000.00");
            assertThat(response.getParceiroId()).isEqualTo(1L);
            verify(pedidoRepository).save(any(Pedido.class));
        }

        @Test
        @DisplayName("Deve rejeitar pedido quando crédito insuficiente")
        void deveRejeitarQuandoCreditoInsuficiente() {
            parceiro.setCreditoDisponivel(new BigDecimal("100.00"));

            CriarPedidoRequest request = new CriarPedidoRequest();
            request.setParceiroId(1L);

            ItemPedidoRequest item = new ItemPedidoRequest();
            item.setProduto("Servidor");
            item.setQuantidade(10);
            item.setPrecoUnitario(new BigDecimal("5000.00"));
            request.setItens(List.of(item));

            when(parceiroRepository.findByIdComLock(1L)).thenReturn(Optional.of(parceiro));

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("Crédito insuficiente");
        }

        @Test
        @DisplayName("Deve lançar exceção quando parceiro não existe")
        void deveLancarExcecaoParceiroInexistente() {
            CriarPedidoRequest request = new CriarPedidoRequest();
            request.setParceiroId(999L);
            request.setItens(List.of());

            when(parceiroRepository.findByIdComLock(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.criar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Parceiro não encontrado");
        }
    }

    @Nested
    @DisplayName("Buscar Pedido")
    class BuscarPedido {

        @Test
        @DisplayName("Deve buscar pedido por ID")
        void deveBuscarPorId() {
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            PedidoResponse response = pedidoService.buscarPorId(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        }

        @Test
        @DisplayName("Deve lançar exceção quando pedido não existe")
        void deveLancarExcecaoPedidoInexistente() {
            when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.buscarPorId(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pedido não encontrado");
        }

        @Test
        @DisplayName("Deve buscar pedidos por status")
        void deveBuscarPorStatus() {
            when(pedidoRepository.findByStatus(StatusPedido.PENDENTE)).thenReturn(List.of(pedido));

            List<PedidoResponse> result = pedidoService.buscarPorStatus(StatusPedido.PENDENTE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(StatusPedido.PENDENTE);
        }

        @Test
        @DisplayName("Deve buscar pedidos por período")
        void deveBuscarPorPeriodo() {
            LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2026, 12, 31, 23, 59);

            when(pedidoRepository.findByCriadoEmBetween(inicio, fim)).thenReturn(List.of(pedido));

            List<PedidoResponse> result = pedidoService.buscarPorPeriodo(inicio, fim);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Atualizar Status")
    class AtualizarStatus {

        @Test
        @DisplayName("Deve aprovar pedido e debitar crédito")
        void deveAprovarEDebitarCredito() {
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(parceiroRepository.findByIdComLock(1L)).thenReturn(Optional.of(parceiro));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            pedidoService.atualizarStatus(1L, StatusPedido.APROVADO);

            assertThat(parceiro.getCreditoDisponivel()).isEqualByComparingTo("2250.00");
            verify(parceiroRepository).save(parceiro);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Deve rejeitar aprovação quando crédito insuficiente")
        void deveRejeitarAprovacaoSemCredito() {
            parceiro.setCreditoDisponivel(new BigDecimal("100.00"));

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(parceiroRepository.findByIdComLock(1L)).thenReturn(Optional.of(parceiro));

            assertThatThrownBy(() -> pedidoService.atualizarStatus(1L, StatusPedido.APROVADO))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("Crédito insuficiente");
        }

        @Test
        @DisplayName("Deve rejeitar transição de status de pedido cancelado")
        void deveRejeitarTransicaoDePedidoCancelado() {
            pedido.setStatus(StatusPedido.CANCELADO);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.atualizarStatus(1L, StatusPedido.APROVADO))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("Não é possível alterar status de pedido cancelado");
        }

        @Test
        @DisplayName("Deve rejeitar transição de status de pedido entregue")
        void deveRejeitarTransicaoDePedidoEntregue() {
            pedido.setStatus(StatusPedido.ENTREGUE);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.atualizarStatus(1L, StatusPedido.ENVIADO))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("Não é possível alterar status de pedido já entregue");
        }

        @Test
        @DisplayName("Deve rejeitar quando status é o mesmo")
        void deveRejeitarMesmoStatus() {
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.atualizarStatus(1L, StatusPedido.PENDENTE))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("Pedido já está com status");
        }
    }

    @Nested
    @DisplayName("Cancelar Pedido")
    class CancelarPedido {

        @Test
        @DisplayName("Deve cancelar pedido aprovado e estornar crédito")
        void deveCancelarAprovadoEEstornarCredito() {
            pedido.setStatus(StatusPedido.APROVADO);
            parceiro.setCreditoDisponivel(new BigDecimal("2250.00"));

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(parceiroRepository.findByIdComLock(1L)).thenReturn(Optional.of(parceiro));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            pedidoService.cancelar(1L);

            assertThat(parceiro.getCreditoDisponivel()).isEqualByComparingTo("10000.00");
            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        }

        @Test
        @DisplayName("Deve cancelar pedido pendente sem estorno")
        void deveCancelarPendenteSemEstorno() {
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            pedidoService.cancelar(1L);

            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
            verify(parceiroRepository, never()).save(any());
        }
    }
}
