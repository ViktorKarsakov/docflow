package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Task;
import kkkvd.docflow.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToAndStatusInOrderByDeadlineAsc(User assignedTo, List<Task.TaskStatus> statuses);

    List<Task> findByAssignedByOrderByCreatedAtDesc(User assignedBy);

    @Query("SELECT t FROM Task t " +
            "WHERE t.deadline < :today " +
            "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Task> findOverdue(@Param("today") LocalDate today);
}
