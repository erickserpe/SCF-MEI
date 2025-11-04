package br.com.ellomei.service;

import br.com.ellomei.domain.Usuario;
import br.com.ellomei.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementação do UserDetailsService do Spring Security.
 *
 * Responsável por carregar os dados do usuário durante a autenticação.
 * Verifica se o email foi verificado antes de permitir o login.
 *
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Service
public class DetalheUsuarioServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(DetalheUsuarioServiceImpl.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("🔍 Tentativa de login do usuário: {}", username);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            logger.warn("❌ Usuário não encontrado: {}", username);
            throw new UsernameNotFoundException("Usuário não encontrado.");
        }

        Usuario usuario = usuarioOpt.get();

        // Verifica se o email foi verificado
        if (!usuario.isEmailVerificado()) {
            logger.warn("⚠️ Tentativa de login com email não verificado: {}", username);
            throw new DisabledException("Email não verificado. Por favor, verifique seu email antes de fazer login.");
        }

        logger.debug("✅ Usuário {} autenticado com sucesso", username);

        // Converte o Set<Role> para List<SimpleGrantedAuthority>
        // O Spring Security espera objetos GrantedAuthority
        // Filtra roles nulas ou com nome nulo para evitar NullPointerException
        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getRoles() != null
                    ? usuario.getRoles().stream()
                            .filter(role -> role != null && role.getNome() != null)
                            .map(role -> new SimpleGrantedAuthority(role.getNome()))
                            .collect(Collectors.toList())
                    : java.util.Collections.emptyList()
        );
    }
}