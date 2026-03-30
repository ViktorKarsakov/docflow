package kkkvd.docflow.service;

import kkkvd.docflow.entities.AuditLog;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Сервис журнала аудита.
// Записывает все ключевые действия пользователей: создание документа,
// отправку на согласование, решения по шагам, поручения и т.д.
// Используется для просмотра истории действий администратором.
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    // Записать событие в журнал аудита.

    // action     — код действия: "DOCUMENT_CREATED", "STEP_APPROVED", "TASK_CANCELLED" и т.д.
    // entityType — тип объекта: "Document", "Task", "User"
    // entityId   — ID объекта в виде строки
    // details    — произвольное описание что произошло
    @Transactional
    public void log(User user, String action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }
}
