package com.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CriarParceiroRequest {

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @NotNull(message = "limiteCredito é obrigatório")
    @Positive(message = "limiteCredito deve ser positivo")
    private BigDecimal limiteCredito;
}
