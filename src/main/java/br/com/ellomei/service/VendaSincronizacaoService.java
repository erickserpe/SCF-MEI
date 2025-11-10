package br.com.ellomei.service;

import br.com.ellomei.dto.VendaSincronizacaoDTO;
import br.com.ellomei.domain.Conta;
import br.com.ellomei.domain.Lancamento;
import br.com.ellomei.domain.TipoLancamento;
import br.com.ellomei.domain.Usuario;
import br.com.ellomei.repository.ContaRepository;
import br.com.ellomei.repository.LancamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável por processar a sincronização de vendas do PDV.
 * 
 * Este serviço:
 * 1. Valida os dados recebidos do PDV
 * 2. Cria lançamentos (receitas) no ElloMEI
 * 3. Atualiza os saldos das contas
 * 4. Retorna o mapeamento de IDs locais → IDs ElloMEI
 */
@Service
public class VendaSincronizacaoService {

    private static final Logger logger = LoggerFactory.getLogger(VendaSincronizacaoService.class);

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private ContaService contaService;

    /**
     * Processa um lote de vendas do PDV e cria os lançamentos correspondentes.
     * 
     * @param vendas Lista de vendas a serem sincronizadas
     * @param usuario Usuário autenticado (dono das vendas)
     * @return Mapa com os IDs locais (do PDV) como chave e os IDs criados no ElloMEI como valor
     */
    @Transactional
    public Map<String, Long> processarVendas(List<VendaSincronizacaoDTO> vendas, Usuario usuario) {
        logger.info("🔄 Iniciando sincronização de {} vendas para o usuário: {}", 
                   vendas.size(), usuario.getUsername());

        Map<String, Long> resultado = new HashMap<>();

        for (VendaSincronizacaoDTO venda : vendas) {
            try {
                // Valida a venda
                validarVenda(venda, usuario);

                // Cria o lançamento
                Lancamento lancamento = criarLancamentoDeVenda(venda, usuario);

                // Salva o lançamento
                Lancamento lancamentoSalvo = lancamentoRepository.save(lancamento);

                // Atualiza o saldo da conta
                atualizarSaldoConta(lancamentoSalvo);

                // Adiciona ao resultado
                resultado.put(venda.idVendaLocal(), lancamentoSalvo.getId());

                logger.info("✅ Venda sincronizada: {} → Lançamento ID: {}", 
                           venda.idVendaLocal(), lancamentoSalvo.getId());

            } catch (Exception e) {
                logger.error("❌ Erro ao processar venda {}: {}", 
                            venda.idVendaLocal(), e.getMessage(), e);
                // Continua processando as outras vendas
                // Em produção, considere adicionar um campo de erro no retorno
            }
        }

        logger.info("🎉 Sincronização concluída: {} vendas processadas com sucesso", resultado.size());
        return resultado;
    }

    /**
     * Valida os dados da venda antes de processar.
     * 
     * @param venda Dados da venda
     * @param usuario Usuário autenticado
     * @throws IllegalArgumentException se os dados forem inválidos
     */
    private void validarVenda(VendaSincronizacaoDTO venda, Usuario usuario) {
        if (venda.idVendaLocal() == null || venda.idVendaLocal().isBlank()) {
            throw new IllegalArgumentException("ID da venda local é obrigatório");
        }

        if (venda.dataVenda() == null) {
            throw new IllegalArgumentException("Data da venda é obrigatória");
        }

        if (venda.valorTotal() == null || venda.valorTotal().signum() <= 0) {
            throw new IllegalArgumentException("Valor total deve ser maior que zero");
        }

        if (venda.idContaDestino() == null) {
            throw new IllegalArgumentException("ID da conta destino é obrigatório");
        }

        // Verifica se a conta existe e pertence ao usuário
        Conta conta = contaRepository.findById(venda.idContaDestino())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Conta não encontrada: " + venda.idContaDestino()));

        if (!conta.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                "Conta não pertence ao usuário autenticado");
        }
    }

    /**
     * Cria um lançamento (receita) a partir dos dados da venda.
     * 
     * @param venda Dados da venda
     * @param usuario Usuário autenticado
     * @return Lançamento criado (ainda não salvo)
     */
    private Lancamento criarLancamentoDeVenda(VendaSincronizacaoDTO venda, Usuario usuario) {
        Lancamento lancamento = new Lancamento();

        // Dados básicos
        lancamento.setTipo(TipoLancamento.ENTRADA);
        lancamento.setData(venda.dataVenda());
        lancamento.setValor(venda.valorTotal());
        lancamento.setUsuario(usuario);

        // Descrição
        String descricao = venda.descricao() != null && !venda.descricao().isBlank()
                ? venda.descricao()
                : "Venda PDV " + venda.idVendaLocal();

        // Adiciona informações do cliente se disponível
        if (venda.cliente() != null && venda.cliente().nome() != null) {
            descricao += " - Cliente: " + venda.cliente().nome();
        }

        lancamento.setDescricao(descricao);

        // Conta destino
        Conta conta = contaRepository.findById(venda.idContaDestino())
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        lancamento.setConta(conta);

        // Grupo de operação (para rastreamento)
        lancamento.setGrupoOperacao("PDV-" + venda.idVendaLocal());

        // Status confirmado (venda já foi realizada)
        lancamento.setStatus(br.com.ellomei.domain.StatusLancamento.PAGO);

        // Sem nota fiscal por padrão (pode ser ajustado)
        lancamento.setComNotaFiscal(false);

        return lancamento;
    }

    /**
     * Atualiza o saldo da conta após criar o lançamento.
     * 
     * @param lancamento Lançamento criado
     */
    private void atualizarSaldoConta(Lancamento lancamento) {
        Conta conta = lancamento.getConta();

        // Para receitas, adiciona ao saldo
        conta.setSaldoAtual(conta.getSaldoAtual().add(lancamento.getValor()));

        contaRepository.save(conta);

        logger.debug("💰 Saldo atualizado - Conta: {} - Novo saldo: {}",
                    conta.getNomeConta(), conta.getSaldoAtual());
    }
}

