package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Substitution;
import kkkvd.docflow.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubstitutionRepository extends JpaRepository<Substitution, Long> {

    // Найти активные замещения для конкретного сотрудника на сегодня.
    // Используется чтобы показать документы отсутствующего его заместителю.
    @Query("""
            SELECT s FROM Substitution s
            WHERE s.originalUser = :user
                AND s.active = true
                AND s.startDate <= :today
                AND s.endDate >= :today
""")
    List<Substitution> findActiveForUser(
            @Param("user") User user,
            @Param("today") LocalDate today);

    // Кого сейчас замещает данный пользователь.
    @Query("""
            SELECT s FROM Substitution s
            WHERE s.substituteUser = :user
                AND s.active = true
                AND s.startDate <= :today
                AND s.endDate >= :today
""")
    List<Substitution> findWhomUserSubstitutes(
            @Param("user") User user,
            @Param("today") LocalDate today);
}
