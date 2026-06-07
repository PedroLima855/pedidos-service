package com.pedido.service;

import com.pedido.dto.CriarParceiroRequest;
import com.pedido.entity.Parceiro;
import com.pedido.repository.ParceiroRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParceiroService {

    private final ParceiroRepository parceiroRepository;

    @Transactional
    public Parceiro criar(CriarParceiroRequest request) {
        Parceiro parceiro = Parceiro.builder()
                .nome(request.getNome())
                .limiteCreditoTotal(request.getLimiteCredito())
                .creditoDisponivel(request.getLimiteCredito())
                .build();
        return parceiroRepository.save(parceiro);
    }

    @Transactional(readOnly = true)
    public Parceiro buscarPorId(Long id) {
        return parceiroRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Parceiro não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Parceiro> listarTodos() {
        return parceiroRepository.findAll();
    }
}
