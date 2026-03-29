package kkkvd.docflow.service;

import kkkvd.docflow.dto.*;
import kkkvd.docflow.entities.*;
import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Главный сервис системы.
// Здесь вся бизнес-логика: создание документа, маршрут согласования,
// решение по шагу, отзыв документа.
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final UserRepository userRepository;
    private final RouteTemplateRepository routeTemplateRepository;
    private final NotificationService notificationService;

    // Создание черновика
    // Пользователь заполнил форму и нажал "Сохранить черновик".
    @Transactional
    public DocumentResponse createDraft(DocumentRequest request, User author) {
        DocumentType type = documentTypeRepository.findById(request.getDocumentTypeId())
                .orElseThrow(() -> new RuntimeException("Тип документа не найден"));
        Document document = new Document();
        document.setDocumentType(type);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setAddressee(request.getAddressee());
        document.setDeadline(request.getDeadline());
        document.setAuthor(author);
        document.setStatus(Document.DocumentStatus.DRAFT);

        return DocumentResponse.fromEntity(documentRepository.save(document));
    }

    // Отправка на согласование
    // Пользователь нажал "Отправить на согласование".
    // Система: 1) строит маршрут по шаблону, 2) активирует первый шаг,
    //          3) уведомляет исполнителя первого шага.
    @Transactional
    public DocumentResponse submit(Long documentId, User currentUser) {
        Document document = getDocumentOrThrow(documentId);

        // Только автор может отправить, только из статусов DRAFT или REJECTED
        checkAuthor(document, currentUser);
        if (document.getStatus() != Document.DocumentStatus.DRAFT && document.getStatus() != Document.DocumentStatus.REJECTED) {
            throw new RuntimeException("Документ нельзя отправить в текущем статусе");
        }

        // Удаляем старые шаги (если это повторная отправка после отклонения)
        approvalStepRepository.deleteAll(approvalStepRepository.findByDocumentOrderByStepOrderAsc(document));

        // Берём шаблон маршрута для этого типа документа
        RouteTemplate template = routeTemplateRepository.findByDocumentTypeAndActiveTrue(document.getDocumentType())
                .orElseThrow(() -> new RuntimeException("Маршрут для типа документа не настроен. Обратитесь к администратору."));

        // Создаём шаги по шаблону
        List<RouteTemplateStep> templateSteps = template.getSteps();
        if (templateSteps.isEmpty()) {
            throw new RuntimeException("Шаблон маршрута не содержит шагов");
        }

        for (RouteTemplateStep templateStep : templateSteps) {
            ApprovalStep step = new ApprovalStep();
            step.setDocument(document);
            step.setStepOrder(templateStep.getStepOrder());
            step.setStepName(templateStep.getStepName());
            step.setStepType(templateStep.getStepType());
            step.setAssignedRole(templateStep.getAssignedRole());
            step.setAssigneeDepartment(templateStep.getAssignedDepartment());
            step.setStatus(ApprovalStep.StepStatus.PENDING);
            approvalStepRepository.save(step);
        }

        // Обновляем документ
        document.setStatus(Document.DocumentStatus.ON_APPROVAL);
        document.setSubmittedAt(LocalDateTime.now());
        document.setSubmissionCount(document.getSubmissionCount() + 1);
        document.setRejectionReason(null);
        documentRepository.save(document);

        // Активируем первый шаг и уведомляем исполнителей
        activateNextStep(document);

        return DocumentResponse.fromEntity(document);
    }

    // Решение по шагу: Согласовать или Отклонить
    @Transactional
    public DocumentResponse processDecision(Long documentId, ApprovalDecisionRequest decision, User currentUser) {
        Document document = getDocumentOrThrow(documentId);

        // Найти активный шаг
        ApprovalStep activeStep = approvalStepRepository.findByDocumentAndStatus(document, ApprovalStep.StepStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Нет активного шага согласования"));

        // Проверить что текущий пользователь имеет право обрабатывать этот шаг
        checkCanProcess(activeStep, currentUser);

        // Если отклоняем — комментарий обязателен
        if (!decision.getApproved() && (decision.getComment() == null || decision.getComment().isBlank())) {
            throw new RuntimeException("При отклонении необходимо указать причину");
        }

        // Сохраняем решение на шаге
        activeStep.setProcessedBy(currentUser);
        activeStep.setComment(decision.getComment());
        activeStep.setProcessedAt(LocalDateTime.now());

        if (decision.getApproved()) {
            activeStep.setStatus(ApprovalStep.StepStatus.APPROVED);
            approvalStepRepository.save(activeStep);

            // Проверяем есть ли следующий шаг
            boolean hasNext = activateNextStep(document);
            if (!hasNext) {
                // Все шаги пройдены — документ согласован
                document.setStatus(Document.DocumentStatus.APPROVED);
                document.setCompletedAt(LocalDateTime.now());
                documentRepository.save(document);

                // Уведомляем автора
                notificationService.send(
                        document.getAuthor(),
                        "Документ согласован",
                        "Ваш документ «" + document.getTitle() + "» полностью согласован.",
                        Notification.NotificationType.INFO,
                        "/documents/" + document.getId()
                );
                notificationService.sendEmail(
                        document.getAuthor().getEmail(),
                        "Документ согласован — " + document.getTitle(),
                        "<p>Ваш документ <b>«" + document.getTitle() + "»</b> полностью согласован.</p>"
                );
            }
        } else {
            // Отклонение
            activeStep.setStatus(ApprovalStep.StepStatus.REJECTED);
            approvalStepRepository.save(activeStep);

            // Помечаем оставшиеся шаги как пропущенные
            List<ApprovalStep> pendingSteps = approvalStepRepository.findByDocumentOrderByStepOrderAsc(document)
                    .stream()
                    .filter(s -> s.getStatus() == ApprovalStep.StepStatus.PENDING)
                    .toList();
            pendingSteps.forEach(s -> s.setStatus(ApprovalStep.StepStatus.SKIPPED));
            approvalStepRepository.saveAll(pendingSteps);

            // Документ возвращается автору
            document.setStatus(Document.DocumentStatus.REJECTED);
            document.setRejectionReason(decision.getComment());
            documentRepository.save(document);

            // Уведомляем автора
            notificationService.send(
                    document.getAuthor(),
                    "Документ отклонён",
                    "Ваш документ «" + document.getTitle() + "» отклонён. Причина: " + decision.getComment(),
                    Notification.NotificationType.WARNING,
                    "/documents/" + document.getId()
            );
            notificationService.sendEmail(
                    document.getAuthor().getEmail(),
                    "Документ отклонён — " + document.getTitle(),
                    "<p>Ваш документ <b>«" + document.getTitle() + "»</b> отклонён.</p>" +
                            "<p>Причина: " + decision.getComment() + "</p>"
            );
        }
        return DocumentResponse.fromEntity(document);
    }

    // Отзыв документа автором
    @Transactional
    public DocumentResponse withdraw(Long documentId, User currentUser) {
        Document document = getDocumentOrThrow(documentId);
        checkAuthor(document, currentUser);

        if (document.getStatus() != Document.DocumentStatus.ON_APPROVAL) {
            throw new RuntimeException("Отозвать можно только документ на согласовании");
        }

        // Сбрасываем активный шаг
        approvalStepRepository.findByDocumentAndStatus(document, ApprovalStep.StepStatus.ACTIVE)
                .ifPresent(s -> {
                    s.setStatus(ApprovalStep.StepStatus.SKIPPED);
                    approvalStepRepository.save(s);
                });
        document.setStatus(Document.DocumentStatus.WITHDRAWN);
        documentRepository.save(document);
        return DocumentResponse.fromEntity(document);
    }

    // Получение документов

    // Расширенный поиск по любым критериям.
    // Пустые поля в запросе просто игнорируются.
    @Transactional(readOnly = true)
    public List<DocumentResponse> search(DocumentSearchRequest request) {
        return documentRepository.findAll(DocumentSpecification.build(request))
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    // Документы текущего пользователя (вкладка "Мои документы")
    @Transactional(readOnly = true)
    public List<DocumentResponse> findMyDocuments(User user) {
        return documentRepository.findByAuthorOrderByCreatedAtDesc(user)
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    // Документы ожидающие действия текущего пользователя (вкладка "Входящие")
    @Transactional(readOnly = true)
    public List<DocumentResponse> findPendingForMe(User user) {
        return documentRepository.findPendingForUser(user, user.getRoles())
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    // Все документы системы (для администратора и главврача)
    @Transactional(readOnly = true)
    public List<DocumentResponse> findAll() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    // Карточка документа с историей согласования
    @Transactional(readOnly = true)
    public DocumentResponse findById(Long id) {
        Document document = getDocumentOrThrow(id);
        DocumentResponse response = DocumentResponse.fromEntity(document);

        // Подгружаем шаги согласования
        List<ApprovalStepResponse> steps = approvalStepRepository.findByDocumentOrderByStepOrderAsc(document)
                .stream()
                .map(ApprovalStepResponse::fromEntity)
                .toList();
        response.setApprovalSteps(steps);
        return response;
    }

    // Вспомогательные методы
    // Активировать следующий PENDING-шаг и уведомить исполнителей.
    // Возвращает true если следующий шаг есть, false если маршрут завершён.
    private boolean activateNextStep(Document document) {
        return approvalStepRepository.findFirstByDocumentAndStatusOrderByStepOrderAsc(document, ApprovalStep.StepStatus.PENDING)
                .map(nextStep -> {
                    nextStep.setStatus(ApprovalStep.StepStatus.ACTIVE);
                    approvalStepRepository.save(nextStep);

                    // Уведомляем всех пользователей с нужной ролью
                    List<User> assignees = userRepository.findByRoleName(nextStep.getAssignedRole().getName());

                    for (User assignee : assignees) {
                        notificationService.send(
                                assignee,
                                "Документ ожидает вашего согласования",
                                "Документ «" + document.getTitle() + "» ожидает вашего решения на шаге: " + nextStep.getStepName(),
                                Notification.NotificationType.ACTION_REQUIRED,
                                "/documents/" + document.getId()
                        );
                        notificationService.sendEmail(
                                assignee.getEmail(),
                                "Требуется согласование — " + document.getTitle(),
                                "<p>Документ <b>«" + document.getTitle() + "»</b> ожидает вашего решения.</p>" +
                                        "<p>Шаг: " + nextStep.getStepName() + "</p>"
                        );
                    }
                    return true;
                })
                .orElse(false);
    }

    private Document getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Документ не найден: " + id));
    }

    private void checkAuthor(Document document, User user) {
        if (!document.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Доступ запрещён: вы не автор документа");
        }
    }

    // Проверить что пользователь может обработать шаг:
    // либо ему назначен конкретно, либо у него есть нужная роль.
    private void checkCanProcess(ApprovalStep step, User user) {
        boolean isAssignedDirectly = step.getAssigneeUser() != null
                && step.getAssigneeUser().getId().equals(user.getId());

        boolean hasRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(step.getAssignedRole().getName()));

        if (!isAssignedDirectly && !hasRole) {
            throw new RuntimeException("Доступ запрещён: у вас нет прав на обработку этого шага");
        }
    }
}
