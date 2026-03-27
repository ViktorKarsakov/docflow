package kkkvd.docflow.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "approval_steps",
        indexes = {
                @Index(name = "idx_approval_step_document", columnList = "document_id"),
                @Index(name = "idx_approval_step_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 200)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepType stepType = StepType.APPROVAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status = StepStatus.PENDING;

    @Column(nullable = false)
    private int stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_role_id", nullable = false)
    private Role assignedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assigneeUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_department_id")
    private Department assigneeDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column
    private LocalDateTime processedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


    public enum StepType {
        APPROVAL("Согласование"),
        SIGNATURE("Подпись"),
        EXECUTION("Исполнение"),
        RESOLUTION("Резолюция");

        private final String displayName;

        StepType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum StepStatus {
        PENDING("Ожидает"),
        ACTIVE("На согласовании"),
        APPROVED("Согласован"),
        REJECTED("Отклонён"),
        SKIPPED("Пропущен");

        private final String displayName;

        StepStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
