package br.com.ellomei.repository;

import br.com.ellomei.domain.EmailVerificationToken;
import br.com.ellomei.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository para gerenciar tokens de verificação de email.
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Busca um token por código.
     * 
     * @param codigo Código de 6 dígitos
     * @return Optional contendo o token se encontrado
     */
    Optional<EmailVerificationToken> findByCodigo(String codigo);

    /**
     * Busca um token válido (não usado e não expirado) por código.
     * 
     * @param codigo Código de 6 dígitos
     * @param agora Data/hora atual para verificar expiração
     * @return Optional contendo o token se encontrado e válido
     */
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.codigo = :codigo AND t.usado = false AND t.dataExpiracao > :agora")
    Optional<EmailVerificationToken> findByCodigoAndValidoTrue(@Param("codigo") String codigo, @Param("agora") LocalDateTime agora);

    /**
     * Busca tokens de um usuário específico.
     * 
     * @param usuario Usuário
     * @return Optional contendo o token mais recente do usuário
     */
    Optional<EmailVerificationToken> findFirstByUsuarioOrderByDataCriacaoDesc(Usuario usuario);

    /**
     * Deleta todos os tokens de um usuário.
     * 
     * @param usuario Usuário
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);

    /**
     * Deleta tokens expirados (limpeza automática).
     * 
     * @param agora Data/hora atual
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.dataExpiracao < :agora")
    void deleteExpiredTokens(@Param("agora") LocalDateTime agora);
}

