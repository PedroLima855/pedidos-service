package com.pedido.repository;

import com.pedido.entity.Pedido;
import com.pedido.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByStatus(StatusPedido status);

    List<Pedido> findByCriadoEmBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Pedido> findByParceiroId(Long parceiroId);
}
