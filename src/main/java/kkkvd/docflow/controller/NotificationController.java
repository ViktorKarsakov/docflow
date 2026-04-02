package kkkvd.docflow.controller;

import kkkvd.docflow.dto.NotificationResponse;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.service.NotificationService;
import kkkvd.docflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Контроллер уведомлений.
// Управляет колокольчиком в шапке — список уведомлений и пометка как прочитанное.
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    // Все уведомления текущего пользователя.
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.getAll(currentUser));
    }

    // Количество непрочитанных уведомлений — для бейджа на колокольчике.
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        long count = notificationService.countUnread(currentUser);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Пометить одно уведомление как прочитанное.
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // Пометить все уведомления как прочитанные.
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findEntityByUsername(userDetails.getUsername());
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.noContent().build();
    }
}
