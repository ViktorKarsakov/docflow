package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentRequest {

    @NotNull(message = "Тип документа обязателен")
    private Long documentTypeId;

    @NotBlank(message = "Тема документа обязательна")
    private String title;

    @NotBlank(message = "Текст документа обязателен")
    private String content;

    private String addressee;

    private LocalDate deadline;

}
