package kkkvd.docflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RouteTemplateRequest {

    @NotBlank(message = "Название обязательно")
    private String name;
    private String description;
    @NotNull(message = "Тип документа обязателен")
    private Long documentTypeId;
    private boolean active = true;
    private List<RouteTemplateStepRequest> steps;
}
