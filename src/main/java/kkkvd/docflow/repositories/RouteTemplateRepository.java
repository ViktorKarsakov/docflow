package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.entities.RouteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RouteTemplateRepository extends JpaRepository<RouteTemplate, Long> {

    Optional<RouteTemplate> findByDocumentTypeAndActiveTrue(DocumentType documentType);
}
