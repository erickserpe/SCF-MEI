package br.com.ellomei.controller;

import br.com.ellomei.config.security.CurrentUser;
import br.com.ellomei.domain.Comprovante;
import br.com.ellomei.domain.Lancamento;
import br.com.ellomei.domain.StatusLancamento;
import br.com.ellomei.domain.TipoLancamento;
import br.com.ellomei.domain.Usuario;
import br.com.ellomei.service.DashboardService;
import br.com.ellomei.service.LancamentoService;
import br.com.ellomei.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller responsável por gerenciar solicitações de relatórios.
 *
 * Gera relatórios em PDF de forma síncrona e retorna diretamente para download.
 *
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private static final Logger logger = LoggerFactory.getLogger(RelatorioController.class);

    @Autowired
    private PdfService pdfService;

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private DashboardService dashboardService;


    /**
     * Gera relatório de faturamento MEI em PDF e retorna para download.
     *
     * @return ResponseEntity com o PDF para download
     */
    @GetMapping("/faturamento/mei/pdf")
    public ResponseEntity<byte[]> gerarRelatorioFaturamentoMEI(
            @RequestParam(required = false) Integer ano,
            @CurrentUser Usuario usuario) {

        try {
            if (ano == null) {
                ano = LocalDate.now().getYear();
            }

            logger.info("Gerando relatório de faturamento MEI para ano {} e usuário: {}", ano, usuario.getUsername());

            // Buscar faturamento oficial e bancário
            BigDecimal faturamentoOficial = dashboardService.getFaturamentoOficial(ano, usuario);
            BigDecimal faturamentoBancario = dashboardService.getFaturamentoBancario(ano, usuario);
            BigDecimal metaFaturamento = dashboardService.getMetaFaturamentoBaseadoEmCustos(ano, usuario);

            // Preparar variáveis para o template
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("ano", ano);
            variaveis.put("faturamentoOficial", faturamentoOficial);
            variaveis.put("faturamentoBancario", faturamentoBancario);
            variaveis.put("metaFaturamento", metaFaturamento);

            // Gerar PDF
            CompletableFuture<byte[]> pdfFuture = pdfService.gerarPdfDeHtml("relatorio_faturamento_mei", variaveis);
            byte[] pdfBytes = pdfFuture.get();

            logger.info("PDF gerado com sucesso. Tamanho: {} KB", pdfBytes.length / 1024);

            // Configurar headers para download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "relatorio_faturamento_mei_" + ano + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de faturamento MEI", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gera relatório de compras com nota fiscal em PDF e retorna para download.
     *
     * @return ResponseEntity com o PDF para download
     */
    @GetMapping("/compras-com-nota/pdf")
    public ResponseEntity<byte[]> gerarRelatorioComprasNota(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long contatoId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) StatusLancamento status,
            @CurrentUser Usuario usuario) {

        try {
            logger.info("Gerando relatório de compras com nota para usuário: {}", usuario.getUsername());

            // Buscar lançamentos de SAÍDA com nota fiscal
            List<Lancamento> lancamentos = lancamentoService.buscarComFiltros(
                    dataInicio, dataFim, contaId, contatoId, TipoLancamento.SAIDA, categoriaId,
                    true, // comNotaFiscal = true
                    descricao, status, usuario
            );

            logger.info("Encontrados {} lançamentos com nota fiscal", lancamentos.size());

            // Calcular total
            BigDecimal total = lancamentos.stream()
                    .map(Lancamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Preparar variáveis para o template
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("lancamentos", lancamentos);
            variaveis.put("total", total);
            variaveis.put("dataInicio", dataInicio);
            variaveis.put("dataFim", dataFim);

            // Gerar PDF
            CompletableFuture<byte[]> pdfFuture = pdfService.gerarPdfDeHtml("relatorio_compras_com_nota", variaveis);
            byte[] pdfBytes = pdfFuture.get();

            logger.info("PDF gerado com sucesso. Tamanho: {} KB", pdfBytes.length / 1024);

            // Configurar headers para download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "relatorio_compras_com_nota.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de compras com nota", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gera relatório de lançamentos em PDF e retorna para download.
     *
     * @return ResponseEntity com o PDF para download
     */
    @GetMapping("/lancamentos/pdf")
    public ResponseEntity<byte[]> gerarRelatorioLancamentos(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long contatoId,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean comNotaFiscal,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) StatusLancamento status,
            @CurrentUser Usuario usuario) {

        try {
            logger.info("Gerando relatório de lançamentos para usuário: {}", usuario.getUsername());

            // Buscar lançamentos com os filtros
            List<Lancamento> lancamentos = lancamentoService.buscarComFiltros(
                    dataInicio, dataFim, contaId, contatoId, tipo, categoriaId,
                    comNotaFiscal, descricao, status, usuario
            );

            logger.info("Encontrados {} lançamentos", lancamentos.size());

            // Processar caminhos dos comprovantes para serem acessíveis no PDF
            for (Lancamento lancamento : lancamentos) {
                for (Comprovante comprovante : lancamento.getComprovantes()) {
                    String currentPath = comprovante.getPathArquivo();
                    if (currentPath != null && !currentPath.startsWith("/uploads/")) {
                        comprovante.setPathArquivo("/uploads/" + currentPath);
                    }
                }
            }

            // Preparar variáveis para o template
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("lancamentos", lancamentos);
            variaveis.put("dataInicio", dataInicio);
            variaveis.put("dataFim", dataFim);

            // Gerar PDF
            CompletableFuture<byte[]> pdfFuture = pdfService.gerarPdfDeHtml("relatorio_lancamentos", variaveis);
            byte[] pdfBytes = pdfFuture.get();

            logger.info("PDF gerado com sucesso. Tamanho: {} KB", pdfBytes.length / 1024);

            // Configurar headers para download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "relatorio_lancamentos.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de lançamentos", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}