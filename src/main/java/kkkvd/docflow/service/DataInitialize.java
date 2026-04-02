package kkkvd.docflow.service;

import kkkvd.docflow.entities.Department;
import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.entities.Role;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

// Инициализация начальных данных.
// Запускается автоматически при старте приложения (реализует ApplicationRunner).
// Проверяет что данные ещё не созданы — и только тогда добавляет.
// Повторный запуск безопасен — дубли не создаются.
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitialize implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PasswordEncoder passwordEncoder;

    // пароль администратора берётся из переменной окружения ADMIN_PASSWORD.
    // Если переменная не задана — используется "236Sync75df" по умолчанию (только для разработки).
    // Перед боевым запуском:
    //   Windows PowerShell: $env:ADMIN_PASSWORD = "сложный_пароль"
    //   Linux:              export ADMIN_PASSWORD=сложный_пароль
    @Value("${app.admin.password:236Sync75df}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Запуск инициализации начальных данных...");
        initRoles();
        initDepartments();
        initDocumentTypes();
        initAdminUser();
        log.info("Инициализация завершена. Войдите как 'admin' и настройте маршруты согласования.");
    }

    // Роли
    private void initRoles() {
        if (roleRepository.count() > 0) return;

        List<Role> roles = List.of(
                createRole("ROLE_ADMIN", "Администратор"),
                createRole("ROLE_CHIEF", "Главный врач"),
                createRole("ROLE_ECONOMIST", "Экономический отдел"),
                createRole("ROLE_HR", "Отдел кадров"),
                createRole("ROLE_ACCOUNTING", "Бухгалтерия"),
                createRole("ROLE_PURCHASE", "Отдел закупок"),
                createRole("ROLE_IT", "Информационно-аналитический отдел"),
                createRole("ROLE_LAWYER", "Юридический отдел"),
                createRole("ROLE_EMPLOYEE", "Сотрудник")
        );
        roleRepository.saveAll(roles);
        log.info("Создано ролей: {}", roles.size());
    }

    private Role createRole(String name, String displayName) {
        Role role = new Role();
        role.setName(name);
        role.setDisplayName(displayName);
        return role;
    }

    // Отделы
    private void initDepartments() {
        if (departmentRepository.count() > 0) return;

        List<Department> departments = List.of(
                createDepartment("ECON", "Экономический отдел", "Красноярск"),
                createDepartment("HR", "Отдел кадров", "Красноярск"),
                createDepartment("BUCH", "Бухгалтерия", "Красноярск"),
                createDepartment("PURCHASE", "Отдел закупок", "Красноярск"),
                createDepartment("IT", "Информационно-аналитический отдел", "Красноярск"),
                createDepartment("LEGAL", "Юридический отдел", "Красноярск"),
                createDepartment("WAREHOUSE", "Склад", "Красноярск"),
                createDepartment("ACHINSK", "Филиал Ачинск", "Ачинск"),
                createDepartment("LESOSIBIRSK", "Филиал Лесосибирск", "Лесосибирск"),
                createDepartment("KANSK", "Филиал Канск", "Канск")
        );
        departmentRepository.saveAll(departments);
        log.info("Создано отделов: {}", departments.size());
    }

    private Department createDepartment(String code, String name, String city) {
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        department.setCity(city);
        return department;
    }

    // Типы документов
    private void initDocumentTypes() {
        if (documentTypeRepository.count() > 0) return;

        List<DocumentType> documentTypes = List.of(
                createDocType("MEMO_WAREHOUSE", "Служебная записка на выдачу со склада", "СЗС", 1),
                createDocType("MEMO_PURCHASE", "Служебная записка на закупку", "СЗЗ", 2),
                createDocType("APPLICATION", "Заявление", "ЗА", 3),
                createDocType("REPORT_NOTE", "Докладная записка", "ДЗ", 4),
                createDocType("MEMO_GENERAL", "Служебная записка (общая)", "СЗ", 5)
        );
        documentTypeRepository.saveAll(documentTypes);
        log.info("Создано типов документов: {}", documentTypes.size());
    }

    private DocumentType createDocType(String code, String displayName, String prefix, int sortOrder) {
        DocumentType dt = new DocumentType();
        dt.setCode(code);
        dt.setDisplayName(displayName);
        dt.setPrefix(prefix);
        dt.setSortOrder(sortOrder);
        dt.setActive(true);
        return dt;
    }

    // Первый администратор
    private void initAdminUser() {
        if (userRepository.count() > 0) return;

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Department itDept = departmentRepository.findByCode("IT").orElseThrow();

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName("Администратор системы");
        admin.setPosition("Инженер-программист");
        admin.setDepartment(itDept);
        admin.setRoles(Set.of(adminRole));
        admin.setActive(true);

        userRepository.save(admin);
        log.info("Создан администратор. Логин: admin — смените пароль после первого входа!");
    }
}
