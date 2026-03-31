package kkkvd.docflow.controller;

import jakarta.validation.Valid;
import kkkvd.docflow.dto.TaskRequest;
import kkkvd.docflow.dto.TaskResponse;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.service.TaskService;
import kkkvd.docflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final UserService userService;

    // Создать поручение.
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request,currentUser));
    }

    // Мои поручения как исполнителя (статусы NEW и IN_PROGRESS).
    @GetMapping("/my")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(taskService.findMyTasks(currentUser));
    }

    // Поручения которые я выдал как руководитель.
    @GetMapping("/issued")
    public ResponseEntity<List<TaskResponse>> getIssuedByMe(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(taskService.findIssuedByMe(currentUser));
    }

    // Взять поручение в работу (статус NEW → IN_PROGRESS).
    @PostMapping("/{id}/start")
    public ResponseEntity<TaskResponse> startProgress(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(taskService.startProgress(id,currentUser));
    }

    // Отметить поручение выполненным.
    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> complete(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        String report = body.get("report");
        return ResponseEntity.ok(taskService.complete(id, report, currentUser));
    }

    // Отменить поручение (только тот кто выдал).
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TaskResponse> cancel(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(taskService.cancel(id, currentUser));
    }
}
