package kkkvd.docflow.dto;

import kkkvd.docflow.entities.RouteTemplate;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RouteTemplateDto {

    private Long id;
    private String name;
    private String description;
    private Long documentTypeId;
    private String documentTypeName;
    private boolean active;
    private List<RouteTemplateStepDto> steps;

    public static RouteTemplateDto fromEntity(RouteTemplate t) {
        List<RouteTemplateStepDto> steps = t.getSteps().stream()
                .map(RouteTemplateStepDto::fromEntity)
                .toList();

        return RouteTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .documentTypeId(t.getDocumentType().getId())
                .documentTypeName(t.getDocumentType().getDisplayName())
                .active(t.isActive())
                .steps(steps)
                .build();
    }
}
