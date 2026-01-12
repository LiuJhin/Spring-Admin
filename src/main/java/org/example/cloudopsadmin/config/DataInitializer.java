package org.example.cloudopsadmin.config;

import org.example.cloudopsadmin.entity.Role;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.entity.UserStatus;
import org.example.cloudopsadmin.entity.Permission;
import org.example.cloudopsadmin.repository.PermissionRepository;
import org.example.cloudopsadmin.repository.RoleRepository;
import org.example.cloudopsadmin.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class DataInitializer {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void init() {
        Role adminRole = roleRepository.findByName("ADMIN");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator");
            roleRepository.save(adminRole);
        }

        Permission accountManage = permissionRepository.findByName("ACCOUNT_MANAGE")
                .orElseGet(() -> {
                    Permission p = new Permission();
                    p.setName("ACCOUNT_MANAGE");
                    p.setDescription("账号管理");
                    return permissionRepository.save(p);
                });

        if (adminRole.getPermissions() == null) {
            adminRole.setPermissions(new ArrayList<>());
        }
        if (adminRole.getPermissions().stream().noneMatch(p -> p != null && "ACCOUNT_MANAGE".equals(p.getName()))) {
            adminRole.getPermissions().add(accountManage);
            roleRepository.save(adminRole);
        }

        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setName("Admin User");
            admin.setStatus(UserStatus.ACTIVE);
            admin.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Admin");
            admin.setRoles(Collections.singletonList(adminRole));
            userRepository.save(admin);
        }
    }
}
