package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findByAuthorOrderByCreatedAtDesc(User author);

    List<Document> findByAuthorAndStatusOrderByCreatedAtDesc(User author, Document.DocumentStatus status);

    @Query("SELECT DISTINCT d FROM Document d " +
            "JOIN ApprovalStep s ON s.document = d " +
            "WHERE s.status = 'ACTIVE' " +
            "AND (" +
            "    s.assigneeUser = :user " +
            "    OR (s.assigneeDepartment = :department AND :department IS NOT NULL) " +
            "    OR (s.assigneeUser IS NULL AND s.assigneeDepartment IS NULL AND s.assignedRole IN :roles)" +
            ") " +
            "ORDER BY d.submittedAt DESC")
    List<Document> findPendingForUser(@Param("user") User user,
                                      @Param("department") Department department,
                                      @Param("roles") Set<Role> roles);

    List<Document> findAllByOrderByCreatedAtDesc();

    @Query("SELECT d FROM Document d " +
            "WHERE d.deadline < :today " +
            "AND d.status NOT IN ('APPROVED', 'COMPLETED', 'REJECTED', 'WITHDRAWN')")
    List<Document> findOverdue(@Param("today")LocalDate today);

    //поиск только персональных шагов — для замещения.
    // Заместитель видит только те документы где шаг назначен
    // конкретному сотруднику (assigneeUser), но не отделу и не роли.
    @Query("SELECT DISTINCT d FROM Document d " +
            "JOIN ApprovalStep s ON s.document = d " +
            "WHERE s.status = 'ACTIVE' " +
            "AND s.assigneeUser = :user " +
            "ORDER BY d.submittedAt DESC")
    List<Document> findPendingPersonalForUser(@Param("user") User user);

    long countByDocumentTypeAndSubmissionCountGreaterThan(DocumentType documentType, int count);
}
