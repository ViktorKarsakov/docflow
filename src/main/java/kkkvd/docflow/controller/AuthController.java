package kkkvd.docflow.controller;

import kkkvd.docflow.dto.UserResponse;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Контроллер аутентификации.
//
// Форму входа Spring Security обрабатывает сам (настроено в SecurityConfig).
// Этот контроллер нужен только для одного: вернуть данные текущего пользователя.
// Фронтенд вызывает /api/auth/me при загрузке страницы, чтобы узнать
// кто сейчас залогинен и какие у него роли.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    // Получить данные текущего авторизованного пользователя.
    // @AuthenticationPrincipal — Spring Security автоматически подставляет
    // объект UserDetails залогиненного пользователя.
    // Фронтенд вызывает этот метод при загрузке любой страницы.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
