package kkkvd.docflow.controller;

import jakarta.validation.Valid;
import kkkvd.docflow.dto.RouteTemplateDto;
import kkkvd.docflow.dto.RouteTemplateRequest;
import kkkvd.docflow.repositories.*;
import kkkvd.docflow.service.RouteTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final RouteTemplateService routeTemplateService;

    // Все шаблоны маршрутов.
    @GetMapping
    public ResponseEntity<List<RouteTemplateDto>> getAll() {
        return ResponseEntity.ok(routeTemplateService.findAll());
    }

    // Получить шаблон по ID (с шагами).
    @GetMapping("/{id}")
    public ResponseEntity<RouteTemplateDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(routeTemplateService.findById(id));
    }

    // Создать новый шаблон маршрута.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RouteTemplateDto> create(@Valid @RequestBody RouteTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeTemplateService.create(request));
    }

    // Обновить шаблон маршрута.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RouteTemplateDto> update(@PathVariable Long id, @Valid @RequestBody RouteTemplateRequest request) {
        return ResponseEntity.ok(routeTemplateService.update(id, request));
    }

    // Удалить шаблон маршрута.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
