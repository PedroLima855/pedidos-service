package com.pedido.service;

import com.pedido.dto.CriarPedidoRequest;
import com.pedido.dto.PedidoResponse;
import com.pedido.entity.ItemPedido;
import com.pedido.entity.Parceiro;
import com.pedido.entity.Pedido;
import com.pedido.enums.StatusPedido;
import com.pedido.event.PedidoStatusAlteradoEvent;
import com.pedido.exception.NegocioException;
import com.pedido.repository.ParceiroRepository;
import com.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ParceiroRepository parceiroRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request) {
        Parceiro parceiro = parceiroRepository.findByIdComLock(request.getParceiroId())
                .orElseThrow(() -> new EntityNotFoundException("Parceiro não encontrado: " + request.getParceiroId()));

        Pedido pedido = Pedido.builder()
                .parceiro(parceiro)
                .valorTotal(BigDecimal.ZERO)
                .build();

        List<ItemPedido> itens = request.getItens().stream().map(item -> ItemPedido.builder()
                .pedido(pedido)
                .produto(item.getProduto())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .build()).toList();

        pedido.setItens(itens);

        BigDecimal valorTotal = itens.stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setValorTotal(valorTotal);

        if (parceiro.getCreditoDisponivel().compareTo(valorTotal) < 0) {
            throw new NegocioException("Crédito insuficiente. Disponível: " + parceiro.getCreditoDisponivel() + ", Necessário: " + valorTotal);
        }

        Pedido salvo = pedidoRepository.save(pedido);
        return PedidoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));
        return PedidoResponse.from(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream().map(PedidoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return pedidoRepository.findByCriadoEmBetween(inicio, fim).stream().map(PedidoResponse::from).toList();
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));

        StatusPedido statusAnterior = pedido.getStatus();

        validarTransicaoStatus(statusAnterior, novoStatus);

        if (novoStatus == StatusPedido.APROVADO) {
            Parceiro parceiro = parceiroRepository.findByIdComLock(pedido.getParceiro().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Parceiro não encontrado"));

            if (parceiro.getCreditoDisponivel().compareTo(pedido.getValorTotal()) < 0) {
                throw new NegocioException("Crédito insuficiente para aprovar o pedido");
            }

            parceiro.setCreditoDisponivel(parceiro.getCreditoDisponivel().subtract(pedido.getValorTotal()));
            parceiroRepository.save(parceiro);
        }

        if (novoStatus == StatusPedido.CANCELADO && statusAnterior == StatusPedido.APROVADO) {
            Parceiro parceiro = parceiroRepository.findByIdComLock(pedido.getParceiro().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Parceiro não encontrado"));

            parceiro.setCreditoDisponivel(parceiro.getCreditoDisponivel().add(pedido.getValorTotal()));
            parceiroRepository.save(parceiro);
        }

        pedido.setStatus(novoStatus);
        Pedido salvo = pedidoRepository.save(pedido);

        eventPublisher.publishEvent(new PedidoStatusAlteradoEvent(this, pedido.getId(), statusAnterior, novoStatus));

        return PedidoResponse.from(salvo);
    }

    @Transactional
    public PedidoResponse cancelar(Long id) {
        return atualizarStatus(id, StatusPedido.CANCELADO);
    }

    private void validarTransicaoStatus(StatusPedido atual, StatusPedido novo) {
        if (atual == StatusPedido.CANCELADO) {
            throw new NegocioException("Não é possível alterar status de pedido cancelado");
        }
        if (atual == StatusPedido.ENTREGUE) {
            throw new NegocioException("Não é possível alterar status de pedido já entregue");
        }
        if (atual == novo) {
            throw new NegocioException("Pedido já está com status " + atual);
        }
    }
}
