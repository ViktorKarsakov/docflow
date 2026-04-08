package kkkvd.docflow.service;

import kkkvd.docflow.dto.RouteTemplateDto;
import kkkvd.docflow.dto.RouteTemplateRequest;
import kkkvd.docflow.dto.RouteTemplateStepRequest;
import kkkvd.docflow.entities.ApprovalStep;
import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.entities.RouteTemplate;
import kkkvd.docflow.entities.RouteTemplateStep;
import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Сервис шаблонов маршрутов согласования.
// Маршрут — это цепочка шагов которую проходит документ перед финальным согласованием.
// Например: Экономический отдел → Главврач → Отдел закупок.
//
// Администратор настраивает маршруты через визуальный конструктор.
// Когда пользователь отправляет документ на согласование, система
// берёт нужный шаблон маршрута и создаёт реальные шаги (ApprovalStep).
@Service
@RequiredArgsConstructor
public class RouteTemplateService {

    private final RouteTemplateRepository routeTemplateRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    // Получить все шаблоны маршрутов.
    // Используется в конструкторе маршрутов — для списка существующих шаблонов.
    @Transactional(readOnly = true)
    public List<RouteTemplateDto> findAll() {
        return routeTemplateRepository.findAll().stream()
                .map(RouteTemplateDto::fromEntity)
                .toList();
    }

    // Получить шаблон по ID со всеми шагами.
    // Используется когда администратор открывает шаблон для редактирования.
    @Transactional(readOnly = true)
    public RouteTemplateDto findById(Long id) {
        RouteTemplate rt = routeTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Шаблон не найден: " + id));
        return RouteTemplateDto.fromEntity(rt);
    }

    // Создать новый шаблон маршрута.
    // Вызывается когда администратор сохраняет новый маршрут из конструктора.
    @Transactional
    public RouteTemplateDto create(RouteTemplateRequest request) {
        // existingId = null — значит создаём новый шаблон
        RouteTemplate template = buildTemplate(null, request);
        return RouteTemplateDto.fromEntity(routeTemplateRepository.save(template));
    }

    // Обновить существующий шаблон маршрута.
    // Вызывается когда администратор изменяет уже сохранённый маршрут.
    @Transactional
    public RouteTemplateDto update(Long id, RouteTemplateRequest request) {
        // Проверяем что шаблон существует перед обновлением
        routeTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Шаблон не найден: " + id));

        RouteTemplate template = buildTemplate(id, request);
        return RouteTemplateDto.fromEntity(routeTemplateRepository.save(template));
    }

    // Удалить шаблон маршрута.
    @Transactional
    public void delete(Long id) {
        routeTemplateRepository.deleteById(id);
    }

    // Вспомогательный метод построения шаблона из запроса
    // existingId — если null, создаём новый объект RouteTemplate.
    // если не null, загружаем существующий и обновляем его.
    private RouteTemplate buildTemplate(Long existingId, RouteTemplateRequest request) {
        // Находим тип документа к которому привязывается маршрут.
        DocumentType type = documentTypeRepository.findById(request.getDocumentTypeId())
                .orElseThrow(() -> new RuntimeException("Тип документа не найден"));

        // Если редактируем — загружаем существующий шаблон.
        // Если создаём — создаём пустой объект
        RouteTemplate template = existingId != null ? routeTemplateRepository.findById(existingId).orElse(new RouteTemplate())
                : new RouteTemplate();

        // Устанавливаем основные поля шаблона
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDocumentType(type);
        template.setActive(request.isActive());

        // Очищаем старые шаги перед добавлением новых.
        template.getSteps().clear();

        if (request.getSteps() != null) {
            List<RouteTemplateStep> steps = new ArrayList<>();

            // Перебираем каждый шаг из запроса (var — это RouteTemplateStepRequest)
            for (var sr : request.getSteps()) {
                //Валидация — у каждого шага должен быть ровно один способ назначения.
                validateAssignment(sr);

                RouteTemplateStep step = new RouteTemplateStep();
                // Привязываем шаг к шаблону (нужно для @ManyToOne в сущности)
                step.setRouteTemplate(template);
                step.setStepOrder(sr.getStepOrder());
                step.setStepName(sr.getStepName());

                // valueOf() преобразует строку "APPROVAL" в enum ApprovalStep.StepType.APPROVAL
                step.setStepType(ApprovalStep.StepType.valueOf(sr.getStepType()));

                // Назначение исполнителя — ровно одно из трёх: роль, отдел или конкретный пользователь.
                // Проверяем каждое поле — если задано, подгружаем объект из БД.
                if (sr.getAssignedRoleId() != null) {
                    step.setAssignedRole(roleRepository.findById(sr.getAssignedRoleId())
                            .orElseThrow(() -> new RuntimeException("Роль не найдена")));
                }

                if (sr.getAssignedDepartmentId() != null) {
                    step.setAssignedDepartment(departmentRepository.findById(sr.getAssignedDepartmentId())
                            .orElseThrow(() -> new RuntimeException("Отдел не найден")));
                }

                if (sr.getAssignedUserId() != null) {
                    step.setAssignedUser(userRepository.findById(sr.getAssignedUserId())
                            .orElseThrow(() -> new RuntimeException("Пользователь не найден")));
                }

                steps.add(step);
            }

            // Добавляем все шаги в шаблон.
            template.getSteps().addAll(steps);
        }

        return template;
    }

    // Проверяет что у шага заполнено ровно одно поле назначения.
    private void validateAssignment(RouteTemplateStepRequest sr) {
        int count = 0;
        if (sr.getAssignedRoleId() != null) count++;
        if (sr.getAssignedDepartmentId() != null) count++;
        if (sr.getAssignedUserId() != null) count++;

        if (count == 0) {
            throw new RuntimeException("Шаг «" + sr.getStepName() + "»: необходимо указать роль, отдел или сотрудника");
        }

        if (count > 1) {
            throw new RuntimeException("Шаг «" + sr.getStepName() + "»: можно указать только один способ назначения — роль, отдел или сотрудника");
        }
    }
}
