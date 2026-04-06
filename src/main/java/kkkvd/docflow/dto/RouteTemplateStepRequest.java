package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO для одного шага при создании/обновлении шаблона маршрута.
@Data
public class RouteTemplateStepRequest {

    @NotBlank(message = "Название шага обязательно")
    private String stepName;
    private int stepOrder;
    private String stepType = "APPROVAL";
    private Long assignedRoleId;
    private Long assignedDepartmentId;
    private Long assignedUserId;
}
