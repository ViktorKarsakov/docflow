package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank(message = "Системное имя роли обязательно")
    private String name;

    @NotBlank(message = "Название роли обязательно")
    private String displayName;
}
