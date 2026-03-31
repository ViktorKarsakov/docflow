package kkkvd.docflow.controller;

import jakarta.validation.Valid;
import kkkvd.docflow.dto.ApprovalDecisionRequest;
import kkkvd.docflow.dto.DocumentRequest;
import kkkvd.docflow.dto.DocumentResponse;
import kkkvd.docflow.dto.DocumentSearchRequest;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.service.DocumentService;
import kkkvd.docflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Контроллер документов.
// Обрабатывает все HTTP-запросы связанные с документами:
// создание, отправка, согласование, поиск, карточка документа.
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private UserService userService;

    // Создание и редактирование

    // Создать черновик документа.
    @PostMapping
    public ResponseEntity<DocumentResponse> createDraft(@Valid @RequestBody DocumentRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        DocumentResponse response = documentService.createDraft(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Действия над документом
    // Отправить документ на согласование.
    @PostMapping("/{id}/submit")
    public ResponseEntity<DocumentResponse> submit(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(documentService.submit(id, currentUser));
    }

    // Принять решение по текущему шагу согласования.
    @PostMapping("/{id}/decision")
    public ResponseEntity<DocumentResponse> processDecision(@PathVariable Long id,
                                                     @Valid @RequestBody ApprovalDecisionRequest decision,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(documentService.processDecision(id, decision, currentUser));
    }

    // Отозвать документ (только автор, только если на согласовании).
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<DocumentResponse> withdraw(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(documentService.withdraw(id, currentUser));
    }

    // Получение документов
    // Мои документы — все документы которые создал текущий пользователь.
    @GetMapping("/my")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(documentService.findMyDocuments(currentUser));
    }

    // Входящие — документы ожидающие действия текущего пользователя.
    // Включает документы по роли, отделу и замещению.
    @GetMapping("/inbox")
    public ResponseEntity<List<DocumentResponse>> getInbox(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(documentService.findPendingForMe(currentUser));
    }

    // Все документы системы — только для администратора и главного врача.
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHIEF')")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.findAll());
    }

    // Карточка документа с историей согласования.
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    // Расширенный поиск по любым критериям.
    // Все параметры необязательны — можно комбинировать любые.
    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> search(DocumentSearchRequest request) {
        return ResponseEntity.ok(documentService.search(request));
    }
}
