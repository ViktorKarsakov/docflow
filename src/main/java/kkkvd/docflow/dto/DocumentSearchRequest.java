package kkkvd.docflow.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// DTO для расширенного поиска документов.
// Все поля необязательны — пользователь заполняет только то что нужно.
// Пустые поля просто игнорируются при фильтрации.
@Data
public class DocumentSearchRequest {

    private String keyword;
    private Long documentTypeId;
    private String status;
    private Long authorId;
    private String authorName;
    private Long authorDepartmentId;
    private String registrationNumber;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate createdFrom;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate createdTo;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate submittedFrom;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate submittedTo;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate deadlineFrom;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate deadlineTo;
    private Boolean overdueOnly;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}
