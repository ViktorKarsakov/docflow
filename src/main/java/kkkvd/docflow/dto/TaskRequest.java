package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// DTO для создания поручения.
// Руководитель заполняет форму — данные приходят сюда.
@Data
public class TaskRequest {

    private Long documentId;

    @NotBlank(message = "Название поручения обязательно")
    private String title;

    private String description;

    @NotNull(message = "Исполнитель обязателен")
    private Long assignedToId;

    @NotNull(message = "Срок исполнения обязателен")
    private LocalDate deadline;
}
