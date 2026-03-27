package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Document;
import kkkvd.docflow.entities.Role;
import kkkvd.docflow.entities.User;
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

    @Query("""
            SELECT DISTINCT d FROM Document d
            JOIN ApprovalStep s ON s.document = d
            WHERE s.status = 'ACTIVE'
                AND (s.assigneeUser = :user
                    OR (s.assigneeUser IS NULL 
                        AND s.assignedRole IN :roles))
            ORDER BY d.submittedAt DESC
""")
    List<Document> findPendingForUser(@Param("user") User user,
                                      @Param("roles")Set<Role> roles);

    List<Document> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT d FROM Document d
            WHERE d.deadline < :today
                AND d.status NOT IN ('APPROVED', 'COMPLETED', 'REJECTED', 'WITHDRAWN')
""")
    List<Document> findOverdue(@Param("today")LocalDate today);
}
