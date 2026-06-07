package com.pedido.event;

import com.pedido.enums.StatusPedido;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PedidoStatusAlteradoEvent extends ApplicationEvent {

    private final Long pedidoId;
    private final StatusPedido statusAnterior;
    private final StatusPedido statusNovo;

    public PedidoStatusAlteradoEvent(Object source, Long pedidoId, StatusPedido statusAnterior, StatusPedido statusNovo) {
        super(source);
        this.pedidoId = pedidoId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
    }
}
