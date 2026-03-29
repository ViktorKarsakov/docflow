package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Department;
import kkkvd.docflow.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByActiveTrue();

    List<User> findByDepartmentAndActiveTrue(Department department);

    @Query("SELECT u FROM User u " +
            "WHERE u.active = true " +
            "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.position) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY u.fullName ASC")
    List<User> searchByName(@Param("query") String query);

    @Query(
            "SELECT u FROM User u JOIN u.roles r WHERE "
            + "r.name = :roleName AND u.active = true"
    )
    List<User> findByRoleName(@Param("roleName") String roleName);
}
