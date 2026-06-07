package com.pedido.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemPedidoRequest {

    @NotBlank(message = "produto é obrigatório")
    private String produto;

    @NotNull(message = "quantidade é obrigatória")
    @Min(value = 1, message = "quantidade mínima é 1")
    private Integer quantidade;

    @NotNull(message = "precoUnitario é obrigatório")
    @Min(value = 0, message = "precoUnitario não pode ser negativo")
    private BigDecimal precoUnitario;
}
