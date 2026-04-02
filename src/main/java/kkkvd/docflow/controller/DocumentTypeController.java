package kkkvd.docflow.controller;

import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.repositories.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Контроллер типов документов.
// Нужен для выпадающего списка "Тип документа" при создании документа.
// Возвращает только активные типы.
@RestController
@RequestMapping("/api/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {
    private final DocumentTypeRepository documentTypeRepository;

    // Все активные типы документов отсортированные для отображения.
    @GetMapping
    public ResponseEntity<List<DocumentType>> getAll() {
        return ResponseEntity.ok(documentTypeRepository.findByActiveTrueOrderBySortOrderAscDisplayNameAsc());
    }
}
