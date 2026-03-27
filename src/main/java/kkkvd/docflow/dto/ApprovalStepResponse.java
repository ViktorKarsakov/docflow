package kkkvd.docflow.dto;

import kkkvd.docflow.entities.ApprovalStep;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalStepResponse {

    private Long id;
    private int stepOrder;
    private String stepName;
    private String stepType;
    private String stepTypeDisplayName;
    private String assignedRoleName;
    private String assignedRoleDisplayName;
    private String assigneeDepartmentName;
    private String status;
    private String statusDisplayName;
    private String processedByFullName;
    private String comment;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public static ApprovalStepResponse fromEntity(ApprovalStep step) {
        return ApprovalStepResponse.builder()
                .id(step.getId())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .stepType(step.getStepType().name())
                .stepTypeDisplayName(step.getStepType().getDisplayName())
                .assignedRoleName(step.getAssignedRole().getName())
                .assignedRoleDisplayName(step.getAssignedRole().getDisplayName())
                .assigneeDepartmentName(step.getAssigneeDepartment() != null ? step.getAssigneeDepartment().getName() : null)
                .status(step.getStatus().name())
                .statusDisplayName(step.getStatus().getDisplayName())
                .processedByFullName(step.getProcessedBy() != null ? step.getProcessedBy().getFullName() : null)
                .comment(step.getComment())
                .processedAt(step.getProcessedAt())
                .createdAt(step.getCreatedAt())
                .build();
    }
}
