package kkkvd.docflow.controller;

import jakarta.validation.Valid;
import kkkvd.docflow.dto.RouteTemplateDto;
import kkkvd.docflow.dto.RouteTemplateRequest;
import kkkvd.docflow.entities.RouteTemplate;
import kkkvd.docflow.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Контроллер шаблонов маршрутов согласования.
// Используется администратором для настройки маршрутов через визуальный конструктор.
@RestController
@RequestMapping("/api/route-templates")
@RequiredArgsConstructor
public class RouteTemplateController {
    private final RouteTemplateRepository routeTemplateRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    // Все шаблоны маршрутов.
    @GetMapping
    public ResponseEntity<List<RouteTemplateDto>> getAll() {

    }

    // Получить шаблон по ID (с шагами).
    @GetMapping("/{id}")
    public ResponseEntity<RouteTemplateDto> getById(@PathVariable Long id) {

    }

    // Создать новый шаблон маршрута.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RouteTemplateDto> create(@Valid @RequestBody RouteTemplateRequest request) {

    }

    // Обновить шаблон маршрута.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RouteTemplateDto> update(@PathVariable Long id, @Valid @RequestBody RouteTemplateRequest request) {

    }

    // Удалить шаблон маршрута.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

    }
}
