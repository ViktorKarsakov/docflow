package kkkvd.docflow.service;

import kkkvd.docflow.entities.Document;
import kkkvd.docflow.entities.Notification;
import kkkvd.docflow.entities.Task;
import kkkvd.docflow.repositories.DocumentRepository;
import kkkvd.docflow.repositories.TaskRepository;
import kkkvd.docflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Планировщик — выполняет задачи автоматически по расписанию.
// Работает благодаря @EnableScheduling в DocflowApplication.
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {
    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Каждый день в 08:00 проверяем просроченные документы.
    // cron = "секунды минуты часы день месяц день_недели"
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkOverdueDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> overdue = documentRepository.findOverdue(today);

        log.info("Проверка просрочек документов: найдено {}", overdue.size());

        for (Document document : overdue) {
            notificationService.send(
                    document.getAuthor(),
                    "Документ просрочен",
                    "Срок по документу «" + document.getTitle() + "» истёк " + document.getDeadline(),
                    Notification.NotificationType.WARNING,
                    "/documents/" + document.getId()
            );
            notificationService.sendEmail(
                    document.getAuthor().getEmail(),
                    "Документ просрочен — " + document.getTitle(),
                    "<p>Срок по документу <b>«" + document.getTitle() + "»</b> истёк " + document.getDeadline() + "</p>"
            );

            // Уведомить главврача — он должен видеть что зависло
            userRepository.findByRoleName("ROLE_CHIEF").forEach(chief -> {
                notificationService.send(
                        chief,
                        "Просроченный документ",
                        "Документ «" + document.getTitle() + "» просрочен. Автор: "
                                + document.getAuthor().getFullName(),
                        Notification.NotificationType.WARNING,
                        "/documents/" + document.getId()
                );
                notificationService.sendEmail(
                        chief.getEmail(),
                        "Просроченный документ — " + document.getTitle(),
                        "<p>Документ <b>«" + document.getTitle() + "»</b> просрочен.</p>" +
                                "<p>Автор: " + document.getAuthor().getFullName() + "</p>"
                );
            });
        }
    }

    // Каждый день в 08:05 проверяем просроченные поручения.
    @Scheduled(cron = "0 5 8 * * *")
    @Transactional
    public void checkOverdueTasks() {
        LocalDate today = LocalDate.now();
        List<Task> overdue = taskRepository.findOverdue(today);

        log.info("Проверка просрочек поручений: найдено {}", overdue.size());

        for (Task task : overdue) {
            // Уведомить исполнителя — внутри системы
            notificationService.send(
                    task.getAssignedTo(),
                    "Поручение просрочено",
                    "Срок по поручению «" + task.getTitle() + "» истёк " + task.getDeadline(),
                    Notification.NotificationType.WARNING,
                    "/tasks/" + task.getId()
            );
            // Уведомить исполнителя — по email
            notificationService.sendEmail(
                    task.getAssignedTo().getEmail(),
                    "Поручение просрочено — " + task.getTitle(),
                    "<p>Срок по поручению <b>«" + task.getTitle() + "»</b> истёк " + task.getDeadline() + "</p>"
            );

            // Уведомить выдавшего поручение (если это не один и тот же человек)
            if (!task.getAssignedBy().getId().equals(task.getAssignedTo().getId())) {
                // Внутри системы
                notificationService.send(
                        task.getAssignedBy(),
                        "Поручение просрочено",
                        "Поручение «" + task.getTitle() + "» не исполнено в срок. Исполнитель: "
                                + task.getAssignedTo().getFullName(),
                        Notification.NotificationType.WARNING,
                        "/tasks/" + task.getId()
                );
                // По email
                notificationService.sendEmail(
                        task.getAssignedBy().getEmail(),
                        "Поручение просрочено — " + task.getTitle(),
                        "<p>Поручение <b>«" + task.getTitle() + "»</b> не исполнено в срок.</p>" +
                                "<p>Исполнитель: " + task.getAssignedTo().getFullName() + "</p>"
                );
            }
        }
    }
}
