package br.com.ellomei.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade que representa um token de verificação de email.
 * 
 * Quando um usuário se registra, um código de 6 dígitos é gerado
 * e enviado para o email informado. O usuário deve inserir este código
 * para confirmar que o email é válido e pertence a ele.
 * 
 * Segurança:
 * - Código expira em 15 minutos
 * - Código de 6 dígitos numéricos
 * - Apenas um código ativo por usuário
 * - Código é deletado após uso
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código de verificação de 6 dígitos.
     */
    @Column(nullable = false, length = 6)
    private String codigo;

    /**
     * Usuário associado a este token.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Data e hora de criação do token.
     */
    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    /**
     * Data e hora de expiração do token (15 minutos após criação).
     */
    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    /**
     * Indica se o token já foi usado.
     */
    @Column(nullable = false)
    private boolean usado = false;

    /**
     * Construtor padrão (necessário para JPA).
     */
    public EmailVerificationToken() {
    }

    /**
     * Construtor que cria um token com código e usuário.
     * Define automaticamente a data de criação e expiração (15 minutos).
     * 
     * @param codigo Código de 6 dígitos
     * @param usuario Usuário associado
     */
    public EmailVerificationToken(String codigo, Usuario usuario) {
        this.codigo = codigo;
        this.usuario = usuario;
        this.dataCriacao = LocalDateTime.now();
        this.dataExpiracao = LocalDateTime.now().plusMinutes(15);
        this.usado = false;
    }

    /**
     * Verifica se o token está expirado.
     * 
     * @return true se o token expirou, false caso contrário
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    /**
     * Verifica se o token é válido (não expirado e não usado).
     * 
     * @return true se o token é válido, false caso contrário
     */
    public boolean isValido() {
        return !isExpirado() && !usado;
    }

    /**
     * Marca o token como usado.
     */
    public void marcarComoUsado() {
        this.usado = true;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }
}

