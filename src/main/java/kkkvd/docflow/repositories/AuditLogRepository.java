package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.AuditLog;
import kkkvd.docflow.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Все действия конкретного пользователя (для журнала аудита).
    List<AuditLog> findByUserOrderByCreatedAtDesc(User user);

    // Все события по конкретному объекту.
    // Пример: findByEntityTypeAndEntityId("Document", "42") — история документа №42.
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);

}
