package kkkvd.docflow.dto;

import kkkvd.docflow.entities.RouteTemplateStep;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteTemplateStepDto {

    private Long id;
    private int stepOrder;
    private String stepName;
    private String stepType;
    private Long assignedRoleId;
    private Long assignedDepartmentId;
    private Long assignedUserId;

    public static RouteTemplateStepDto fromEntity(RouteTemplateStep s) {
        return RouteTemplateStepDto.builder()
                .id(s.getId())
                .stepOrder(s.getStepOrder())
                .stepName(s.getStepName())
                .stepType(s.getStepType().name())
                .assignedRoleId(s.getAssignedRole() != null ? s.getAssignedRole().getId() : null)
                .assignedDepartmentId(s.getAssignedDepartment() != null ? s.getAssignedDepartment().getId() : null)
                .assignedUserId(s.getAssignedUser() != null ? s.getAssignedUser().getId() : null)
                .build();
    }
}
