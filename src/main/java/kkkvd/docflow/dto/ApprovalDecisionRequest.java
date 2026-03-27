package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// DTO для решения по шагу согласования.
// Отправляется когда пользователь нажимает "Согласовать" или "Отклонить".
@Data
public class ApprovalDecisionRequest {

    @NotNull(message = "Решение обязательно")
    private Boolean approved;

    private String comment;
}
