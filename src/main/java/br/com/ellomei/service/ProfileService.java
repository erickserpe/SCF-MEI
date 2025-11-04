package br.com.ellomei.service;

import br.com.ellomei.domain.Usuario;
import br.com.ellomei.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pelo gerenciamento do perfil do usuário.
 *
 * Centraliza a lógica de atualização de dados pessoais, senha e configurações
 * da conta, garantindo segurança e validação adequadas.
 */
@Service
@Transactional
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private CategoriaDespesaRepository categoriaDespesaRepository;

    @Autowired
    private AssinaturaRepository assinaturaRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Atualiza os dados pessoais do usuário de forma segura.
     *
     * Este método copia apenas os campos permitidos do formulário para o usuário logado,
     * evitando que campos sensíveis (id, password, roles, plano) sejam alterados
     * por requisições maliciosas.
     *
     * @param usuarioLogado Usuário autenticado (do SecurityContext)
     * @param dadosFormulario Dados enviados pelo formulário
     * @return Usuário atualizado
     */
    public Usuario atualizarDadosPessoais(Usuario usuarioLogado, Usuario dadosFormulario) {
        // Copia apenas os campos permitidos para atualização
        usuarioLogado.setNomeCompleto(dadosFormulario.getNomeCompleto());
        usuarioLogado.setEmail(dadosFormulario.getEmail());
        usuarioLogado.setCpf(dadosFormulario.getCpf());
        usuarioLogado.setRazaoSocial(dadosFormulario.getRazaoSocial());
        usuarioLogado.setNomeFantasia(dadosFormulario.getNomeFantasia());
        usuarioLogado.setCnpj(dadosFormulario.getCnpj());
        usuarioLogado.setDataAberturaMei(dadosFormulario.getDataAberturaMei());

        // Salva as alterações
        return usuarioRepository.save(usuarioLogado);
    }

    /**
     * Atualiza a senha do usuário.
     *
     * @param usuarioLogado Usuário autenticado
     * @param senhaAtual Senha atual para validação
     * @param novaSenha Nova senha a ser definida
     * @return true se a senha foi atualizada com sucesso, false se a senha atual estiver incorreta
     */
    public boolean atualizarSenha(Usuario usuarioLogado, String senhaAtual, String novaSenha) {
        // Valida a senha atual
        if (!passwordEncoder.matches(senhaAtual, usuarioLogado.getPassword())) {
            return false;
        }

        // Criptografa e salva a nova senha
        usuarioLogado.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuarioLogado);
        return true;
    }

    /**
     * Busca o usuário atualizado do banco de dados.
     *
     * Útil para recarregar os dados do usuário após atualizações.
     *
     * @param id ID do usuário
     * @return Usuário atualizado
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    /**
     * Valida se a senha fornecida corresponde à senha do usuário.
     *
     * @param usuario Usuário
     * @param senha Senha a ser validada
     * @return true se a senha estiver correta
     */
    public boolean validarSenha(Usuario usuario, String senha) {
        return passwordEncoder.matches(senha, usuario.getPassword());
    }

    /**
     * Exclui permanentemente a conta do usuário e todos os seus dados.
     *
     * @param usuario Usuário a ser excluído
     */
    public void excluirConta(Usuario usuario) {
        logger.info("Iniciando exclusão da conta do usuário: {}", usuario.getEmail());

        try {
            // 1. Excluir todos os lançamentos
            logger.debug("Excluindo lançamentos do usuário...");
            contaRepository.findByUsuario(usuario).forEach(conta -> {
                // Deletar lançamentos associados a cada conta
                lancamentoRepository.deleteAll(
                    lancamentoRepository.findComFiltros(null, null, conta.getId(), null, null, null, null, null, null, usuario)
                );
            });

            // 2. Excluir todas as contas
            logger.debug("Excluindo contas do usuário...");
            contaRepository.deleteAll(contaRepository.findByUsuario(usuario));

            // 3. Excluir todos os contatos
            logger.debug("Excluindo contatos do usuário...");
            contatoRepository.deleteAll(contatoRepository.findByUsuario(usuario));

            // 4. Excluir todas as categorias personalizadas
            logger.debug("Excluindo categorias do usuário...");
            categoriaDespesaRepository.deleteAll(categoriaDespesaRepository.findByUsuario(usuario));

            // 5. Excluir todas as assinaturas
            logger.debug("Excluindo assinaturas do usuário...");
            assinaturaRepository.deleteAll(assinaturaRepository.findByUsuarioOrderByDataInicioDesc(usuario));

            // 6. Excluir tokens de verificação de email
            logger.debug("Excluindo tokens de verificação de email...");
            emailVerificationTokenRepository.deleteByUsuario(usuario);

            // 7. Excluir tokens de recuperação de senha
            logger.debug("Excluindo tokens de recuperação de senha...");
            passwordResetTokenRepository.findByUsuario(usuario).forEach(token -> {
                passwordResetTokenRepository.delete(token);
            });

            // 8. Finalmente, excluir o usuário
            logger.debug("Excluindo usuário...");
            usuarioRepository.delete(usuario);

            logger.info("Conta do usuário {} excluída com sucesso!", usuario.getEmail());
        } catch (Exception e) {
            logger.error("Erro ao excluir conta do usuário {}: {}", usuario.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Erro ao excluir conta: " + e.getMessage(), e);
        }
    }
}

