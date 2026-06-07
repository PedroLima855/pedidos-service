package com.pedido.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificacaoListener {

    @Async
    @EventListener
    public void onStatusAlterado(PedidoStatusAlteradoEvent event) {
        log.info("[NOTIFICAÇÃO] Pedido #{} teve status alterado: {} -> {}",
                event.getPedidoId(), event.getStatusAnterior(), event.getStatusNovo());

        // Simula envio para sistema de mensageria (SQS, Kafka, etc.)
        log.info("[MENSAGERIA] Mensagem enviada para fila de notificações - Pedido #{}", event.getPedidoId());
    }
}
