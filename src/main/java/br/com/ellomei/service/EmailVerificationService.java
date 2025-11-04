package br.com.ellomei.service;

import br.com.ellomei.domain.EmailVerificationToken;
import br.com.ellomei.domain.Usuario;
import br.com.ellomei.repository.EmailVerificationTokenRepository;
import br.com.ellomei.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço responsável pela verificação de email dos usuários.
 * 
 * Funcionalidades:
 * - Gerar código de verificação de 6 dígitos
 * - Enviar código por email
 * - Validar código informado pelo usuário
 * - Marcar email como verificado
 * - Limpar tokens expirados automaticamente
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Gera um código de verificação de 6 dígitos.
     * 
     * @return Código de 6 dígitos (ex: "123456")
     */
    private String gerarCodigo() {
        int codigo = 100000 + random.nextInt(900000); // Gera número entre 100000 e 999999
        return String.valueOf(codigo);
    }

    /**
     * Cria e envia um código de verificação para o email do usuário.
     * 
     * @param usuario Usuário que receberá o código
     * @return O token criado
     */
    @Transactional
    public EmailVerificationToken criarEEnviarCodigo(Usuario usuario) {
        logger.info("📧 Criando código de verificação para usuário: {}", usuario.getUsername());

        // Remove tokens antigos do usuário
        tokenRepository.deleteByUsuario(usuario);

        // Gera novo código
        String codigo = gerarCodigo();

        // Cria o token
        EmailVerificationToken token = new EmailVerificationToken(codigo, usuario);
        token = tokenRepository.save(token);

        logger.info("✅ Código de verificação criado: {} (expira em 15 minutos)", codigo);

        // Envia o código por email
        try {
            emailService.enviarEmailVerificacao(usuario.getEmail(), usuario.getNomeCompleto(), codigo);
            logger.info("📨 Email de verificação enviado para: {}", usuario.getEmail());
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar email de verificação: {}", e.getMessage(), e);
            // Não lança exceção para não bloquear o registro
        }

        return token;
    }

    /**
     * Verifica se um código é válido e marca o email como verificado.
     * 
     * @param codigo Código de 6 dígitos informado pelo usuário
     * @return true se o código foi validado com sucesso, false caso contrário
     */
    @Transactional
    public boolean verificarCodigo(String codigo) {
        logger.info("🔍 Verificando código: {}", codigo);

        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByCodigoAndValidoTrue(codigo, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            logger.warn("⚠️ Código inválido ou expirado: {}", codigo);
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();
        Usuario usuario = token.getUsuario();

        // Marca o token como usado
        token.marcarComoUsado();
        tokenRepository.save(token);

        // Marca o email como verificado
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        logger.info("✅ Email verificado com sucesso para usuário: {}", usuario.getUsername());

        return true;
    }

    /**
     * Reenvia o código de verificação para um usuário.
     * 
     * @param email Email do usuário
     * @return true se o código foi reenviado, false se o usuário não foi encontrado
     */
    @Transactional
    public boolean reenviarCodigo(String email) {
        logger.info("🔄 Reenviando código de verificação para: {}", email);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            logger.warn("⚠️ Usuário não encontrado com email: {}", email);
            return false;
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.isEmailVerificado()) {
            logger.info("ℹ️ Email já verificado para usuário: {}", usuario.getUsername());
            return false;
        }

        criarEEnviarCodigo(usuario);
        return true;
    }

    /**
     * Limpa tokens expirados do banco de dados.
     * Executado automaticamente a cada hora.
     */
    @Scheduled(cron = "0 0 * * * *") // A cada hora
    @Transactional
    public void limparTokensExpirados() {
        logger.info("🧹 Limpando tokens de verificação expirados...");
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("✅ Tokens expirados removidos");
    }

    /**
     * Verifica se um usuário tem email verificado.
     * 
     * @param usuario Usuário a verificar
     * @return true se o email está verificado, false caso contrário
     */
    public boolean isEmailVerificado(Usuario usuario) {
        return usuario.isEmailVerificado();
    }

    /**
     * Busca o token mais recente de um usuário.
     * 
     * @param usuario Usuário
     * @return Optional contendo o token se encontrado
     */
    public Optional<EmailVerificationToken> buscarTokenDoUsuario(Usuario usuario) {
        return tokenRepository.findFirstByUsuarioOrderByDataCriacaoDesc(usuario);
    }
}

