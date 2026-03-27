package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.ApprovalStep;
import kkkvd.docflow.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByDocumentOrderByStepOrderAsc(Document document);

    Optional<ApprovalStep> findByDocumentAndStatus(Document document, ApprovalStep.StepStatus stepStatus);

    Optional<ApprovalStep> findFirstByDocumentAndStatusOrderByStepOrderAsc(Document document, ApprovalStep.StepStatus stepStatus);

}
