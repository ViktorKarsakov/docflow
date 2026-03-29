package kkkvd.docflow.service;

import kkkvd.docflow.dto.UserRequest;
import kkkvd.docflow.dto.UserResponse;
import kkkvd.docflow.entities.Department;
import kkkvd.docflow.entities.Role;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.repositories.DepartmentRepository;
import kkkvd.docflow.repositories.RoleRepository;
import kkkvd.docflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Сервис управления пользователями.
// Используется администратором: создание, редактирование, деактивация.
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    // Поиск пользователей по частичному ФИО/логину/должности.
    // Используется для автодополнения в полях выбора пользователя.
    // Минимум 2 символа — чтобы не возвращать всю базу при случайном нажатии.

    @Transactional(readOnly = true)
    public List<UserResponse> search(String query) {
        if (query == null || query.length() < 2) {
            return List.of();
        }
        return userRepository.searchByName(query).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    // Все пользователи системы (для таблицы администратора).
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    // Найти пользователя по ID.
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        return UserResponse.fromEntity(user);
    }

    // Создать нового пользователя (только администратор).
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Логин уже занят: " + request.getUsername());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        // Хешируем пароль — никогда не храним открытым текстом
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPosition(request.getPosition());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Отдел не найден"));
            user.setDepartment(department);
        }

        user.setRoles(resolveRoles(request.getRoles()));
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    // Редактировать пользователя.
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));

        user.setFullName(request.getFullName());
        user.setPosition(request.getPosition());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Пароль меняем только если передан
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Отдел не найден"));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        user.setRoles(resolveRoles(request.getRoles()));
        return UserResponse.fromEntity(userRepository.save(user));
    }

    // Деактивировать пользователя (не удаляем — сохраняем историю документов).
    @Transactional
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    // Найти реальный объект User по username — нужен в контроллерах.
    @Transactional(readOnly = true)
    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }

    // Вспомогательный метод: преобразовать имена ролей в объекты Role.
    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        if (roleNames == null) return roles;
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));
            roles.add(role);
        }
        return roles;
    }

}
