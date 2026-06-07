package com.pedido.controller;

import com.pedido.dto.CriarParceiroRequest;
import com.pedido.entity.Parceiro;
import com.pedido.service.ParceiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
public class ParceiroController {

    private final ParceiroService parceiroService;

    @PostMapping
    public ResponseEntity<Parceiro> criar(@Valid @RequestBody CriarParceiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parceiroService.criar(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parceiro> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(parceiroService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Parceiro>> listarTodos() {
        return ResponseEntity.ok(parceiroService.listarTodos());
    }
}
