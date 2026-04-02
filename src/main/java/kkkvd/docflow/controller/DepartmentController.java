package kkkvd.docflow.controller;

import kkkvd.docflow.entities.Department;
import kkkvd.docflow.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Контроллер отделов.
// Нужен для заполнения выпадающих списков на фронтенде:
// при создании пользователя, при поиске документов по отделу.
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentRepository departmentRepository;

    // Все отделы отсортированные по названию.
    @GetMapping
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentRepository.findAllByOrderByNameAsc());
    }
}
