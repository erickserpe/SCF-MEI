package br.com.ellomei.controller;

import br.com.ellomei.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller responsável pela verificação de email dos usuários.
 * 
 * Fluxo:
 * 1. Usuário se registra
 * 2. Recebe código de 6 dígitos por email
 * 3. Acessa /verificar-email
 * 4. Insere o código
 * 5. Email é marcado como verificado
 * 6. Usuário pode fazer login
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Controller
@RequestMapping("/verificar-email")
public class EmailVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);

    @Autowired
    private EmailVerificationService emailVerificationService;

    /**
     * Exibe a página de verificação de email.
     */
    @GetMapping
    public String mostrarPaginaVerificacao(Model model) {
        logger.info("📄 Exibindo página de verificação de email");
        return "verificar-email";
    }

    /**
     * Processa o código de verificação informado pelo usuário.
     */
    @PostMapping
    public String verificarCodigo(
            @RequestParam("codigo") String codigo,
            RedirectAttributes redirectAttributes) {

        logger.info("🔍 Tentativa de verificação com código: {}", codigo);

        // Remove espaços e caracteres não numéricos
        codigo = codigo.replaceAll("[^0-9]", "");

        // Valida formato do código
        if (codigo.length() != 6) {
            logger.warn("⚠️ Código inválido (tamanho incorreto): {}", codigo);
            redirectAttributes.addFlashAttribute("mensagemErro", 
                "❌ Código inválido! O código deve ter 6 dígitos.");
            return "redirect:/verificar-email";
        }

        // Verifica o código
        boolean verificado = emailVerificationService.verificarCodigo(codigo);

        if (verificado) {
            logger.info("✅ Email verificado com sucesso!");
            redirectAttributes.addFlashAttribute("mensagemSucesso", 
                "✅ Email verificado com sucesso! Você já pode fazer login.");
            return "redirect:/login";
        } else {
            logger.warn("❌ Código inválido ou expirado: {}", codigo);
            redirectAttributes.addFlashAttribute("mensagemErro", 
                "❌ Código inválido ou expirado! Verifique o código e tente novamente.");
            return "redirect:/verificar-email";
        }
    }

    /**
     * Reenvia o código de verificação para o email do usuário.
     */
    @PostMapping("/reenviar")
    public String reenviarCodigo(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        logger.info("🔄 Solicitação de reenvio de código para: {}", email);

        boolean reenviado = emailVerificationService.reenviarCodigo(email);

        if (reenviado) {
            logger.info("✅ Código reenviado com sucesso para: {}", email);
            redirectAttributes.addFlashAttribute("mensagemSucesso", 
                "✅ Código reenviado! Verifique seu email.");
            return "redirect:/verificar-email";
        } else {
            logger.warn("⚠️ Não foi possível reenviar código para: {}", email);
            redirectAttributes.addFlashAttribute("mensagemErro", 
                "❌ Email não encontrado ou já verificado.");
            return "redirect:/verificar-email";
        }
    }
}

