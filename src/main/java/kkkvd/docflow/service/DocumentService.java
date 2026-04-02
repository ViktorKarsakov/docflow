package kkkvd.docflow.service;

import kkkvd.docflow.dto.*;
import kkkvd.docflow.entities.*;
import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private final SubstitutionRepository substitutionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

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

        Document saved = documentRepository.save(document);

        //запись в журнал аудита
        auditLogService.log(author, "DOCUMENT_CREATED", "Document", saved.getId().toString(),
                "Создан черновик: " + saved.getTitle());

        return DocumentResponse.fromEntity(saved);
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
            step.setAssigneeUser(templateStep.getAssignedUser());
            step.setAssignedRole(templateStep.getAssignedRole());
            step.setAssigneeDepartment(templateStep.getAssignedDepartment());
            step.setStatus(ApprovalStep.StepStatus.PENDING);
            approvalStepRepository.save(step);
        }

        //генерация регистрационного номера при первой отправке.
        // Формат: ПРЕФИКС-ГОД-ПОРЯДКОВЫЙ_НОМЕР. Например: СЗ-2026-000042
        // Номер присваивается только один раз — при первой отправке.
        if (document.getRegistrationNumber() == null) {
            String prefix = document.getDocumentType().getPrefix();
            int year = Year.now().getValue();
            long count = documentRepository.countByDocumentTypeAndSubmissionCountGreaterThan(document.getDocumentType(), 0);
            String regNumber = String.format("%s-%d-%06d", prefix, year, count + 1);
            document.setRegistrationNumber(regNumber);
        }

        // Обновляем документ
        document.setStatus(Document.DocumentStatus.ON_APPROVAL);
        document.setSubmittedAt(LocalDateTime.now());
        document.setSubmissionCount(document.getSubmissionCount() + 1);
        document.setRejectionReason(null);
        documentRepository.save(document);

        //Запись в аудит
        auditLogService.log(currentUser, "DOCUMENT_SUBMITTED", "Document",
                document.getId().toString(),
                "Документ отправлен на согласование. Отправка №" + document.getSubmissionCount());

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

            //Аудит
            auditLogService.log(currentUser, "STEP_APPROVED", "Document",
                    document.getId().toString(),
                    "Шаг «" + activeStep.getStepName() + "» согласован");

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

            //Аудит
            auditLogService.log(currentUser, "STEP_REJECTED", "Document",
                    document.getId().toString(),
                    "Шаг «" + activeStep.getStepName() + "» отклонён. Причина: " + decision.getComment());

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

        //Аудит
        auditLogService.log(currentUser, "DOCUMENT_WITHDRAWN", "Document",
                document.getId().toString(), "Документ отозван автором");

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
        // Сценарий 1: обычные входящие текущего пользователя
        Set<Document> documents = new LinkedHashSet<>(
                documentRepository.findPendingForUser(
                        user, user.getDepartment(), user.getRoles())
        );

        // Сценарий 2: персональные шаги тех кого замещает пользователь
        substitutionRepository.findWhomUserSubstitutes(user, LocalDate.now())
                .forEach(s -> documents.addAll(
                        documentRepository.findPendingPersonalForUser(s.getOriginalUser())
                ));

        return documents.stream()
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
        return approvalStepRepository
                .findFirstByDocumentAndStatusOrderByStepOrderAsc(document, ApprovalStep.StepStatus.PENDING)
                .map(nextStep -> {
                    nextStep.setStatus(ApprovalStep.StepStatus.ACTIVE);
                    approvalStepRepository.save(nextStep);

                    //теперь исполнители определяются через resolveStepAssignees —
                    // сначала конкретный пользователь, потом отдел, потом роль.
                    List<User> assignees = resolveStepAssignees(nextStep);

                    for (User assignee : assignees) {
                        notificationService.send(
                                assignee,
                                "Документ ожидает вашего согласования",
                                "Документ «" + document.getTitle() + "» ожидает вашего решения на шаге: "
                                        + nextStep.getStepName(),
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

    //определяет кто является исполнителем шага.
    //Приоритет: конкретный пользователь → отдел → роль.
    //После определения базового списка применяется замещение:
    //если исполнитель отсутствует и у него есть заместитель — добавляем заместителя.
    private List<User> resolveStepAssignees(ApprovalStep step) {
        List<User> base;

        if (step.getAssigneeUser() != null) {
            // 1. Назначен конкретный пользователь
            base = List.of(step.getAssigneeUser());
        } else if (step.getAssigneeDepartment() != null) {
            // 2. Назначен отдел — берём всех активных сотрудников отдела
            base = userRepository.findByDepartmentAndActiveTrue(step.getAssigneeDepartment());
        } else {
            // 3. Назначена роль — берём всех пользователей с этой ролью
            base = userRepository.findByRoleName(step.getAssignedRole().getName());
        }

        // Применяем замещение: для каждого исполнителя проверяем
        // есть ли у него активный заместитель на сегодня
        Set<User> result = new LinkedHashSet<>();
        LocalDate today = LocalDate.now();

        for (User user : base) {
            List<Substitution> activeSubstitutions =
                    substitutionRepository.findActiveForUser(user, today);

            if (activeSubstitutions.isEmpty()) {
                // Замещения нет — добавляем самого пользователя
                result.add(user);
            } else {
                // Есть заместитель — уведомляем заместителя вместо оригинала
                activeSubstitutions.forEach(s -> result.add(s.getSubstituteUser()));
            }
        }

        return new ArrayList<>(result);
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

    // Проверяет, может ли пользователь обработать шаг:
    // 1) шаг назначен ему лично;
    // 2) шаг назначен его отделу (если шаг не персональный);
    // 3) шаг назначен по его роли (если не указаны пользователь и отдел);
    // 4) пользователь является активным заместителем сотрудника, которому назначен персональный шаг.
    private void checkCanProcess(ApprovalStep step, User user) {
        // 1. Прямое назначение конкретному пользователю
        boolean isAssignedDirectly = step.getAssigneeUser() != null
                && step.getAssigneeUser().getId().equals(user.getId());

        // 2. Пользователь из назначенного отдела
        boolean isFromDepartment = step.getAssigneeUser() == null
                && step.getAssigneeDepartment() != null
                && user.getDepartment() != null
                && step.getAssigneeDepartment().getId().equals(user.getDepartment().getId());

        // 3. У пользователя есть нужная роль
        boolean hasRole = step.getAssigneeUser() == null
                && step.getAssigneeDepartment() == null
                && user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(step.getAssignedRole().getName()));

        // 4. Пользователь является заместителем кого-то из исполнителей
        boolean isSubstituteForAssignedUser = step.getAssigneeUser() != null
                && substitutionRepository.findWhomUserSubstitutes(user, LocalDate.now())
                .stream()
                .anyMatch(s -> s.getOriginalUser().getId()
                        .equals(step.getAssigneeUser().getId()));

        if (!isAssignedDirectly && !isFromDepartment && !hasRole && !isSubstituteForAssignedUser) {
            throw new RuntimeException("Доступ запрещён: у вас нет прав на обработку этого шага");
        }
    }
}
