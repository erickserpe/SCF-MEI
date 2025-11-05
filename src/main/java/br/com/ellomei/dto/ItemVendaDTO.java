package br.com.ellomei.dto;

import java.math.BigDecimal;

public record ItemVendaDTO(
    String nomeProduto,
    int quantidade,
    BigDecimal valorUnitario
) {}

