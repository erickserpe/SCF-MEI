package br.com.ellomei.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method-level security with @PreAuthorize
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @Order(1) // A cadeia da API deve vir primeiro
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(new AntPathRequestMatcher("/api/**")) // Aplica esta regra apenas para /api/
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated() // Todas as rotas /api/** exigem autenticação
            )
            .httpBasic(Customizer.withDefaults()) // Usa HTTP Basic Auth
            .csrf(AbstractHttpConfigurer::disable); // Desabilita CSRF para a API
        return http.build();
    }

    @Bean
    @Order(2) // A cadeia da Web (login por formulário) vem depois
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        // Esta é a configuração original, agora aplicada a todas as OUTRAS rotas
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Permitir acesso público à landing page, login, registro, verificação de email, recuperação de senha e recursos estáticos
                        .requestMatchers("/", "/home", "/demo", "/login", "/registro",
                                        "/verificar-email", "/verificar-email/**",
                                        "/recuperar-senha", "/recuperar-senha/**",
                                        "/webjars/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // Todas as outras rotas requerem autenticação
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        // Redirecionar usuários autenticados para o dashboard
                        .defaultSuccessUrl("/dashboard", true)
                )
                .logout(logout -> logout
                        // Após logout, redirecionar para a landing page
                        .logoutSuccessUrl("/?logout")
                );
        return http.build();
    }
}
