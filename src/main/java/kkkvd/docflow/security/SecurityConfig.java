package kkkvd.docflow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Конфигурация Spring Security.
// Определяет: кто может открыть какие URL, как выглядит форма входа,
// как хранить пароли и т.д.
// @EnableMethodSecurity — позволяет использовать @PreAuthorize на методах контроллеров.
// Например: @PreAuthorize("hasRole('ADMIN')") — только администратор.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;

    // Главное правило безопасности: кому что разрешено.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Статические файлы (CSS, JS, картинки) — доступны всем без входа
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Страница входа — доступна всем
                        .requestMatchers("/pages/login.html").permitAll()
                        // Всё остальное — только авторизованным пользователям
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                // API запросы получают 401 JSON, а не редирект на страницу
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                            } else {
                                // Остальные запросы редиректятся на логин
                                response.sendRedirect("/pages/login.html");
                            }
                        })
                )
                .formLogin(form -> form
                        // URL страницы входа (фронтенд отдаёт /static/login.html, а Spring слушает здесь)
                        .loginPage("/pages/login.html")
                        // URL куда фронтенд отправляет форму (POST с username и password)
                        .loginProcessingUrl("/login")
                        // Куда перенаправить после успешного входа
                        .defaultSuccessUrl("/pages/dashboard.html", true)
                        // Куда перенаправить при неверном логине/пароле
                        .failureUrl("/pages/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/pages/login.html?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    // BCrypt — надёжный алгоритм хеширования паролей.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
