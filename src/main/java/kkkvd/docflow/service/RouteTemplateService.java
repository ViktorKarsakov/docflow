package kkkvd.docflow.service;

import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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


}
