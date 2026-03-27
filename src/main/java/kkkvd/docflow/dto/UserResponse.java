package kkkvd.docflow.dto;

import kkkvd.docflow.entities.User;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String position;
    private String email;
    private String phone;
    private String departmentName;
    private Long departmentId;
    private Set<String> roles;
    private boolean active;

    // Конвертация User → UserResponse.
    // Вызывается в сервисах: UserResponse.fromEntity(user)
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .position(user.getPosition())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentName(user.getDepartment().getName() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .roles(user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toSet()))
                .active(user.isActive())
                .build();
    }
}
