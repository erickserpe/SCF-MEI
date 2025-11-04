package br.com.ellomei.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method-level security with @PreAuthorize
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // As regras de filtro
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
