package kkkvd.docflow.dto;

import kkkvd.docflow.entities.Task;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private Long documentId;
    private String documentTitle;
    private String title;
    private String description;
    private String assignedByFullName;
    private String assignedToFullName;
    private Long assignedToId;
    private LocalDate deadline;
    private boolean overdue;
    private String status;
    private String statusDisplayName;
    private String report;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static TaskResponse fromEntity(Task task){
        return TaskResponse.builder()
                .id(task.getId())
                .documentId(task.getDocument() != null ? task.getDocument().getId() : null)
                .documentTitle(task.getDocument() != null ? task.getDocument().getTitle() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .assignedByFullName(task.getAssignedBy().getFullName())
                .assignedToFullName(task.getAssignedTo().getFullName())
                .assignedToId(task.getAssignedTo().getId())
                .deadline(task.getDeadline())
                .overdue(task.isOverdue())
                .status(task.getStatus().name())
                .statusDisplayName(task.getStatus().getDisplayName())
                .report(task.getReport())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
