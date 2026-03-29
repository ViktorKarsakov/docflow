package kkkvd.docflow.repositories;

import jakarta.persistence.criteria.Predicate;
import kkkvd.docflow.dto.DocumentSearchRequest;
import kkkvd.docflow.entities.Document;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DocumentSpecification {
    private DocumentSpecification() {}

    public static Specification<Document> build(DocumentSearchRequest request){
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Поиск по теме или тексту документа
            if (hasValue(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
            }

            // Фильтр по типу документа
            if (request.getDocumentTypeId() != null) {
                predicates.add(cb.equal(
                        root.get("documentType").get("id"), request.getDocumentTypeId()
                ));
            }

            // Фильтр по статусу
            if (hasValue(request.getStatus())) {
                try {
                    Document.DocumentStatus status = Document.DocumentStatus.valueOf(request.getStatus());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {}
            }

            // Фильтр по ID автора
            if (request.getAuthorId() != null) {
                predicates.add(cb.equal(
                        root.get("author").get("id"), request.getAuthorId()
                ));
            }

            // Поиск по частичному ФИО автора
            if (hasValue(request.getAuthorName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("author").get("fullName")),
                        "%" + request.getAuthorName().toLowerCase() + "%"
                ));
            }

            // Фильтр по отделу автора
            if (request.getAuthorDepartmentId() != null) {
                predicates.add(cb.equal(
                        root.get("author").get("department").get("id"), request.getAuthorDepartmentId()
                ));
            }

            // Поиск по регистрационному номеру
            if (hasValue(request.getRegistrationNumber())) {
                predicates.add(cb.like(
                        cb.lower(root.get("registrationNumber")),
                        "%" + request.getRegistrationNumber().toLowerCase() + "%"
                ));
            }

            // Диапазон дат создания
            if (request.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt").as(LocalDate.class), request.getCreatedFrom()
                ));
            }
            if (request.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt").as(LocalDate.class), request.getCreatedTo()
                ));
            }

            // Диапазон дат отправки
            if (request.getSubmittedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("submittedAt").as(LocalDate.class), request.getSubmittedFrom()
                ));
            }
            if (request.getSubmittedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("submittedAt").as(LocalDate.class), request.getSubmittedTo()
                ));
            }

            // Диапазон дедлайнов
            if (request.getDeadlineFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("deadline").as(LocalDate.class), request.getDeadlineFrom()
                ));
            }
            if (request.getDeadlineTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("deadline").as(LocalDate.class), request.getDeadlineTo()
                ));
            }

            // Только просроченные
            if (Boolean.TRUE.equals(request.getOverdueOnly())) {
                predicates.add(cb.lessThan(root.get("deadline"), LocalDate.now()));
                predicates.add(root.get("status").in(
                        Document.DocumentStatus.DRAFT,
                        Document.DocumentStatus.APPROVED,
                        Document.DocumentStatus.ON_EXECUTION
                ));
            }

            // Сортировка
            String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortDirection());
            List<String> allowed = List.of(
                    "createdAt", "updatedAt", "title", "status", "deadline", "submittedAt"
            );
            if (allowed.contains(sortBy)) {
                query.orderBy(isAsc ? cb.asc(root.get(sortBy)) : cb.desc(root.get(sortBy)));
            } else {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(predicates.toArray(new Predicate[0])));
        };
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
