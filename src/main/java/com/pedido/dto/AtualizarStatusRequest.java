package com.pedido.dto;

import com.pedido.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtualizarStatusRequest {

    @NotNull(message = "status é obrigatório")
    private StatusPedido status;
}
