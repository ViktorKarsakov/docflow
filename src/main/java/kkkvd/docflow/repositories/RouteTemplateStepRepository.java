package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Role;
import kkkvd.docflow.entities.RouteTemplateStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteTemplateStepRepository extends JpaRepository<RouteTemplateStep, Long> {
    long countByAssignedRole(Role role);
}
