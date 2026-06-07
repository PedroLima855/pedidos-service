package com.pedido.dto;

import com.pedido.entity.Pedido;
import com.pedido.enums.StatusPedido;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PedidoResponse {

    private Long id;
    private Long parceiroId;
    private String parceiroNome;
    private BigDecimal valorTotal;
    private StatusPedido status;
    private List<ItemResponse> itens;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static PedidoResponse from(Pedido pedido) {
        return PedidoResponse.builder()
                .id(pedido.getId())
                .parceiroId(pedido.getParceiro().getId())
                .parceiroNome(pedido.getParceiro().getNome())
                .valorTotal(pedido.getValorTotal())
                .status(pedido.getStatus())
                .itens(pedido.getItens().stream().map(i -> ItemResponse.builder()
                        .produto(i.getProduto())
                        .quantidade(i.getQuantidade())
                        .precoUnitario(i.getPrecoUnitario())
                        .build()).toList())
                .criadoEm(pedido.getCriadoEm())
                .atualizadoEm(pedido.getAtualizadoEm())
                .build();
    }

    @Data
    @Builder
    public static class ItemResponse {
        private String produto;
        private Integer quantidade;
        private BigDecimal precoUnitario;
    }
}
