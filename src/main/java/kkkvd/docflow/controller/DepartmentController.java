package kkkvd.docflow.controller;

import kkkvd.docflow.entities.Department;
import kkkvd.docflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Контроллер отделов.
// Нужен для заполнения выпадающих списков на фронтенде:
// при создании пользователя, при поиске документов по отделу.
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {


    private final DepartmentService departmentService;

    // Все отделы отсортированные по названию.
    @GetMapping
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    // Создать отдел (только администратор).
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> create(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(body));
    }

    // Обновить отдел (только администратор).
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(departmentService.update(id, body));
    }

    //Деактивировать отдел.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.deactivate(id));
    }
}
