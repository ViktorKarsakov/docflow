package kkkvd.docflow.service;

import kkkvd.docflow.entities.Department;
import kkkvd.docflow.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Сервис управления отделами.
// Администратор может добавлять и редактировать отделы через интерфейс.
// Отделы используются в профилях пользователей и в шагах маршрутов согласования.
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    // Получить все отделы отсортированные по названию.
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findByActiveTrueOrderByNameAsc();
    }

    // Создать новый отдел.
    @Transactional
    public Department create(Map<String, String> body) {
        //Проверяем что код отдела уникален
        String code = body.get("code");
        if (code != null && !code.isBlank()) {
            departmentRepository.findByCode(code).ifPresent(existing -> {
                throw new RuntimeException("Отдел с кодом '" + code + "' уже существует");
            });
        }

        String name = body.get("name");
        if (name != null && !name.isBlank()) {
            departmentRepository.findByName(name).ifPresent(existing -> {
                throw new RuntimeException("Отдел с названием '" + name + "' уже существует");
            });
        }

        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setCity(body.get("city"));
        return departmentRepository.save(department);
    }

    // Обновить существующий отдел.
    @Transactional
    public Department update(Long id, Map<String, String> body) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отдел не найден: " + id));

        department.setName(body.get("name"));
        department.setCode(body.get("code"));
        department.setCity(body.get("city"));
        return departmentRepository.save(department);
    }

    //Деактивировать отдел.
    public Department deactivate(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отдел не найден: " + id));
        dept.setActive(false);
        return departmentRepository.save(dept);
    }
}
