package kkkvd.docflow.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_document_status", columnList = "status"),
                @Index(name = "idx_document_author", columnList = "author_id"),
                @Index(name = "idx_document_type", columnList = "document_type_id"),
                @Index(name = "idx_document_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true, name = "registration_number")
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(length = 300)
    private String addressee;

    @Column
    private LocalDate deadline;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false)
    private int submissionCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        return deadline != null
                && LocalDate.now().isAfter(deadline)
                && status != DocumentStatus.APPROVED
                && status != DocumentStatus.COMPLETED
                && status != DocumentStatus.REJECTED
                && status != DocumentStatus.WITHDRAWN;
    }

    public enum DocumentStatus {
        DRAFT("Черновик"),
        ON_APPROVAL("На согласовании"),
        APPROVED("Согласован"),
        ON_EXECUTION("На исполнении"),
        COMPLETED("Завершён"),
        REJECTED("Отклонён"),
        WITHDRAWN("Отозван");

        private final String displayName;

        DocumentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
