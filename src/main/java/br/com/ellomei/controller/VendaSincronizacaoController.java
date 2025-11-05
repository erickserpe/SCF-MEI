package br.com.ellomei.controller;

import br.com.ellomei.config.security.CurrentUser;
import br.com.ellomei.domain.Usuario;
import br.com.ellomei.dto.VendaSincronizacaoDTO;
import br.com.ellomei.service.VendaSincronizacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para sincronização de vendas do PDV (Ponto de Venda).
 * 
 * Este controller recebe lotes de vendas de sistemas externos (PDV, apps mobile, etc.)
 * e as sincroniza com o ElloMEI, criando os lançamentos financeiros correspondentes.
 * 
 * Autenticação: HTTP Basic Auth (configurado em SecurityConfig)
 * Endpoint: POST /api/v1/sincronizacao/vendas
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sincronizacao")
public class VendaSincronizacaoController {

    @Autowired
    private VendaSincronizacaoService vendaSincronizacaoService;

    /**
     * Endpoint para receber um lote de vendas do PDV e sincronizar.
     * 
     * Autenticado via HTTP Basic Auth.
     * 
     * @param vendas Lista de vendas a serem sincronizadas
     * @param usuarioLogado Usuário autenticado (injetado automaticamente via @CurrentUser)
     * @return Mapa com os IDs locais (do PDV) como chave e os IDs criados no ElloMEI como valor
     * 
     * Exemplo de requisição:
     * POST /api/v1/sincronizacao/vendas
     * Authorization: Basic dXNlcjpwYXNz
     * Content-Type: application/json
     * 
     * [
     *   {
     *     "idVendaLocal": "PDV-001",
     *     "dataVenda": "2025-11-05",
     *     "valorTotal": 150.00,
     *     "idContaDestino": 1,
     *     "descricao": "Venda PDV #001",
     *     "cliente": {
     *       "nome": "João Silva",
     *       "cpf": "123.456.789-00",
     *       "cnpj": null
     *     },
     *     "itens": [
     *       {
     *         "nomeProduto": "Produto A",
     *         "quantidade": 2,
     *         "valorUnitario": 75.00
     *       }
     *     ]
     *   }
     * ]
     * 
     * Exemplo de resposta:
     * {
     *   "PDV-001": 123,
     *   "PDV-002": 124
     * }
     */
    @PostMapping("/vendas")
    public ResponseEntity<Map<String, Long>> sincronizarVendas(
            @RequestBody List<VendaSincronizacaoDTO> vendas,
            @CurrentUser Usuario usuarioLogado) {
        
        // A lógica será implementada no serviço (Prompt 3)
        Map<String, Long> resultado = vendaSincronizacaoService.processarVendas(vendas, usuarioLogado);
        
        return ResponseEntity.ok(resultado);
    }
}

