package br.com.ellomei.domain;

import br.com.ellomei.validation.anotations.CPF;
import br.com.ellomei.validation.anotations.CNPJ;
import br.com.ellomei.validation.anotations.EmailValido;
import br.com.ellomei.validation.anotations.SenhaForte;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados de Acesso
    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre 3 e 50 caracteres")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Formato de email inválido")
    @EmailValido(message = "Email inválido ou inexistente")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @SenhaForte(message = "A senha não atende aos requisitos de segurança")
    private String password;

    // Dados Pessoais
    private String nomeCompleto;
    @CPF
    private String cpf;

    // Dados da Empresa (MEI)
    private String razaoSocial;
    private String nomeFantasia;
    @CNPJ
    private String cnpj;

    // Relacionamento ManyToMany com Role
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    private LocalDate dataAberturaMei;

    // Plano de Assinatura (SaaS)
    @Enumerated(EnumType.STRING)
    @NotNull
    private PlanoAssinatura plano = PlanoAssinatura.FREE;

    // Verificação de Email
    @Column(nullable = false)
    private boolean emailVerificado = false;

    // --- CORREÇÃO: A RELAÇÃO INCORRETA FOI REMOVIDA DAQUI ---

    // Getters e Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public LocalDate getDataAberturaMei() {
        return dataAberturaMei;
    }
    public void setDataAberturaMei(LocalDate dataAberturaMei) {
        this.dataAberturaMei = dataAberturaMei;
    }
    public PlanoAssinatura getPlano() {
        return plano;
    }
    public void setPlano(PlanoAssinatura plano) {
        this.plano = plano;
    }
    public boolean isEmailVerificado() {
        return emailVerificado;
    }
    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }
}