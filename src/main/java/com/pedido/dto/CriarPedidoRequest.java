package com.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CriarPedidoRequest {

    @NotNull(message = "parceiroId é obrigatório")
    private Long parceiroId;

    @NotEmpty(message = "itens não pode ser vazio")
    @Valid
    private List<ItemPedidoRequest> itens;
}
