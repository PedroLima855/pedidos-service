package com.pedido.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Table(name = "parceiro")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Parceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "limite_credito", nullable = false)
    private BigDecimal limiteCreditoTotal;

    @Column(name = "credito_disponivel", nullable = false)
    private BigDecimal creditoDisponivel;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
