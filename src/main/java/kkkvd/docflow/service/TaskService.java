package kkkvd.docflow.service;

import kkkvd.docflow.dto.TaskRequest;
import kkkvd.docflow.dto.TaskResponse;
import kkkvd.docflow.entities.Document;
import kkkvd.docflow.entities.Notification;
import kkkvd.docflow.entities.Task;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.repositories.DocumentRepository;
import kkkvd.docflow.repositories.TaskRepository;
import kkkvd.docflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Сервис поручений.
// Руководитель создаёт поручения, исполнитель их видит и отчитывается.
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final NotificationService notificationService;

    // Создать поручение.
    @Transactional
    public TaskResponse create(TaskRequest request, User assignedBy) {
        User assignedTo = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new RuntimeException("Исполнитель не найден"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setAssignedBy(assignedBy);
        task.setAssignedTo(assignedTo);
        task.setDeadline(request.getDeadline());
        task.setStatus(Task.TaskStatus.NEW);

        if (request.getDocumentId() != null) {
            Document document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Документ не найден"));
            task.setDocument(document);
        }

        Task saved = taskRepository.save(task);

        // Уведомить исполнителя
        notificationService.send(
                assignedTo,
                "Новое поручение",
                "Вам выдано поручение: «" + task.getTitle() + "». Срок: " + task.getDeadline(),
                Notification.NotificationType.ACTION_REQUIRED,
                "/tasks/" + saved.getId()
        );
        notificationService.sendEmail(
                assignedTo.getEmail(),
                "Новое поручение — " + task.getTitle(),
                "<p>Вам выдано поручение: <b>«" + task.getTitle() + "»</b></p>" +
                        "<p>Срок исполнения: " + task.getDeadline() + "</p>"
        );

        return TaskResponse.fromEntity(saved);
    }

    // Мои активные поручения (как исполнителя).
    @Transactional(readOnly = true)
    public List<TaskResponse> findMyTasks(User user) {
        return taskRepository.findByAssignedToAndStatusInOrderByDeadlineAsc(user, List.of(Task.TaskStatus.NEW, Task.TaskStatus.IN_PROGRESS))
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    // Поручения выданные текущим пользователем (как руководителя).
    @Transactional(readOnly = true)
    public List<TaskResponse> findIssuedByMe(User user) {
        return taskRepository.findByAssignedByOrderByCreatedAtDesc(user)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    // Взять поручение в работу.
    @Transactional
    public TaskResponse startProgress(Long taskId, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        checkAssignee(task, currentUser);
        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    // Отметить поручение выполненным (с отчётом).
    @Transactional
    public TaskResponse complete(Long taskId, String report, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        checkAssignee(task, currentUser);
        task.setStatus(Task.TaskStatus.COMPLETED);
        task.setReport(report);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Уведомить руководителя об исполнении
        notificationService.send(
                task.getAssignedBy(),
                "Поручение исполнено",
                "Поручение «" + task.getTitle() + "» отмечено выполненным.",
                Notification.NotificationType.INFO,
                "/tasks/" + task.getId()
        );
        notificationService.sendEmail(
                task.getAssignedBy().getEmail(),
                "Поручение исполнено — " + task.getTitle(),
                "<p>Поручение <b>«" + task.getTitle() + "»</b> отмечено выполненным.</p>" +
                        "<p>Исполнитель: " + task.getAssignedTo().getFullName() + "</p>" +
                        "<p>Отчёт: " + (report != null ? report : "не указан") + "</p>"
        );

        return TaskResponse.fromEntity(task);
    }

    // Отменить поручение (только выдавший).
    @Transactional
    public TaskResponse cancel(Long taskId, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        if (!task.getAssignedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Отменить поручение может только тот, кто его выдал");
        }
        task.setStatus(Task.TaskStatus.CANCELLED);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Поручение не найдено: " + id));
    }

    private void checkAssignee(Task task, User user) {
        if (!task.getAssignedTo().getId().equals(user.getId())) {
            throw new RuntimeException("Доступ запрещён: вы не исполнитель этого поручения");
        }
    }
}
