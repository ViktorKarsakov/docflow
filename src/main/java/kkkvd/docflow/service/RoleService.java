package kkkvd.docflow.service;

import kkkvd.docflow.dto.CreateRoleRequest;
import kkkvd.docflow.entities.Role;
import kkkvd.docflow.repositories.RoleRepository;
import kkkvd.docflow.repositories.RouteTemplateStepRepository;
import kkkvd.docflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

// Сервис управления ролями.
@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RouteTemplateStepRepository routeTemplateStepRepository;
    private static final Set<String> PROTECTED_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_CHIEF", "ROLE_EMPLOYEE"
    );

    // Получить все роли.
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    // Создать роль
    @Transactional
    public Role create(CreateRoleRequest request) {
        String name = request.getName().trim();

        // Приводим к стандартному виду
        if (!name.startsWith("ROLE_")) {
            name = "ROLE_" + name.toUpperCase();
        } else {
            name = name.toUpperCase();
        }

        String finalName = name;
        roleRepository.findByName(finalName).ifPresent(r -> {
            throw new RuntimeException("Роль с именем '" + finalName + "' уже существует");
        });

        Role role = new Role();
        role.setName(finalName);
        role.setDisplayName(request.getDisplayName().trim());
        return roleRepository.save(role);
    }

    // Удалить роль.
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + id));

        if (PROTECTED_ROLES.contains(role.getName())) {
            throw new RuntimeException("Роль «" + role.getDisplayName() + "» является системной и не может быть удалена");
        }

        //проверяем что роль не используется у пользователей
        long usersWithRole = userRepository.countByRolesContaining(role);
        if (usersWithRole > 0) {
            throw new RuntimeException(
                    "Роль «" + role.getDisplayName() + "» назначена " + usersWithRole + " пользователю(-ям). Сначала снимите роль с пользователей.");
        }

        // проверяем что роль не используется в шаблонах маршрутов
        long stepsWithRole = routeTemplateStepRepository.countByAssignedRole(role);
        if (stepsWithRole > 0) {
            throw new RuntimeException(
                    "Роль «" + role.getDisplayName() + "» используется в " + stepsWithRole + " шаге(-ах) маршрутов. Сначала обновите маршруты.");
        }

        roleRepository.deleteById(id);
    }
}
