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
                .authorizeHttpRequests(auth -> auth
                        // Статические файлы (CSS, JS, картинки) — доступны всем без входа
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Страница входа — доступна всем
                        .requestMatchers("/login", "/login/**").permitAll()
                        // Всё остальное — только авторизованным пользователям
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // URL страницы входа (фронтенд отдаёт /static/login.html, а Spring слушает здесь)
                        .loginPage("/login")
                        // URL куда фронтенд отправляет форму (POST с username и password)
                        .loginProcessingUrl("/login")
                        // Куда перенаправить после успешного входа
                        .defaultSuccessUrl("/", true)
                        // Куда перенаправить при неверном логине/пароле
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                // Отключаем CSRF только для REST API эндпоинтов (фронтенд на HTML+JS)
                // Для полноценного REST API CSRF обычно отключают полностью
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

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
