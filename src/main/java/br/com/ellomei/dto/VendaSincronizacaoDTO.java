package br.com.ellomei.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VendaSincronizacaoDTO(
    String idVendaLocal, // ID do PDV (para rastreamento)
    LocalDate dataVenda,
    BigDecimal valorTotal,
    Long idContaDestino, // ID da 'Conta' no ElloMEI (ex: "Caixa PDV")
    String descricao, // Descrição da Venda (ex: "Venda PDV #1023")
    ClienteVendaDTO cliente, // Opcional
    List<ItemVendaDTO> itens // Opcional (para log/detalhamento)
) {}

