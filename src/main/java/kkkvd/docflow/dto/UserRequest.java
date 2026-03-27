package kkkvd.docflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

// DTO для создания и редактирования пользователя.
// Принимается от фронтенда через POST/PUT запрос.
@Data
public class UserRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин от 3 до 50 символов")
    private String username;

    // Пароль обязателен только при создании.
    // При редактировании — если пустой, пароль не меняется.
    @Size(min = 6, message = "Пароль минимум 6 символов")
    private String password;

    @NotBlank(message = "ФИО обязательно")
    private String fullName;

    @NotBlank(message = "Должность обязательна")
    private String position;

    @Email(message = "Некорректный формат email")
    private String email;

    private String phone;

    private Long departmentId;

    private Set<String> roles;
}
