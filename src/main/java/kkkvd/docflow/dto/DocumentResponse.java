package kkkvd.docflow.dto;

import kkkvd.docflow.entities.Document;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String registrationNumber;
    private String documentTypeName;
    private String title;
    private String content;
    private String status;
    private String statusDisplayName;
    private String authorFullName;
    private Long authorId;
    private String addressee;
    private LocalDate deadline;
    private boolean overdue;
    private String rejectionReason;
    private int submissionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    // История согласования — список шагов.
    // Заполняется только для карточки документа, не для списка.
    private List<ApprovalStepResponse> approvalSteps;

    public static DocumentResponse fromEntity(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .registrationNumber(document.getRegistrationNumber())
                .documentTypeName(document.getDocumentType().getDisplayName())
                .title(document.getTitle())
                .content(document.getContent())
                .status(document.getStatus().name())
                .statusDisplayName(document.getStatus().getDisplayName())
                .authorFullName(document.getAuthor().getFullName())
                .authorId(document.getAuthor().getId())
                .addressee(document.getAddressee())
                .deadline(document.getDeadline())
                .overdue(document.isOverdue())
                .rejectionReason(document.getRejectionReason())
                .submissionCount(document.getSubmissionCount())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .submittedAt(document.getSubmittedAt())
                .completedAt(document.getCompletedAt())
                .build();
    }
}
