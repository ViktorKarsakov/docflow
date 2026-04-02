package kkkvd.docflow.controller;

import jakarta.validation.Valid;
import kkkvd.docflow.dto.UserRequest;
import kkkvd.docflow.dto.UserResponse;
import kkkvd.docflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Контроллер управления пользователями.
// Все методы кроме /search доступны только администратору.
// /search доступен всем авторизованным — нужен для автодополнения в формах.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // Все пользователи системы — для таблицы в панели администратора.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    // Получить пользователя по ID.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // Создать нового пользователя.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    // Обновить данные пользователя.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    // Деактивировать пользователя (не удаляем — сохраняем историю документов).
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // Поиск пользователей по частичному ФИО, логину или должности.
    // Доступен всем — используется для автодополнения в полях "Исполнитель", "Автор".
    // Минимум 2 символа — проверка в UserService.
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(userService.search(query));
    }
}
