package kkkvd.docflow.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

// Глобальный обработчик ошибок.
// Перехватывает исключения из всех контроллеров и возвращает
// понятный JSON с описанием ошибки вместо стандартной страницы Spring.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // RuntimeException — бизнес-ошибки из сервисов.
    // Например: "Документ не найден", "Доступ запрещён", "Маршрут не настроен".
    // Возвращаем HTTP 400 Bad Request.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handlerRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    // AccessDeniedException — Spring Security бросает когда пользователь
    // пытается обратиться к методу защищённому @PreAuthorize.
    // Возвращаем HTTP 403 Forbidden.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handlerAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Доступ запрещен"));
    }

    // MethodArgumentNotValidException — срабатывает когда @Valid не прошёл проверку.
    // Например: пустое обязательное поле, неверный формат email.
    // Собираем все ошибки полей и возвращаем HTTP 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // Всё остальное — непредвиденные ошибки.
    // Возвращаем HTTP 500 и общее сообщение (без деталей — для безопасности).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера"));
    }
}
