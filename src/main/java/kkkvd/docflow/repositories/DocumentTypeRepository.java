package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByCode(String code);

    List<DocumentType> findByActiveTrueOrderBySortOrderAscDisplayNameAsc();
}
