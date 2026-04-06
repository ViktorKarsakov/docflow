package kkkvd.docflow.controller;

import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Контроллер типов документов.
// Нужен для выпадающего списка "Тип документа" при создании документа.
// Возвращает только активные типы.
@RestController
@RequestMapping("/api/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {
    private final DocumentTypeService documentTypeService;

    // Активные типы — для выпадающего списка при создании документа.
    @GetMapping
    public ResponseEntity<List<DocumentType>> getActive() {
        return ResponseEntity.ok(documentTypeService.findActive());
    }

    // Все типы включая неактивные — для страницы администратора.
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentType>> getAll() {
        return ResponseEntity.ok(documentTypeService.findAll());
    }

    // Получить тип по ID.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentTypeService.findById(id));
    }

    // Создать тип документа.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> create(@RequestBody DocumentType body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentTypeService.create(body));
    }

    // Обновить тип документа.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> update(@PathVariable Long id, @RequestBody DocumentType body) {
        return ResponseEntity.ok(documentTypeService.update(id, body));
    }

    // Изменить активность типа.
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(documentTypeService.setActive(id, active));
    }
}
