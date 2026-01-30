package org.example.cloudopsadmin.config;

import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.Role;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.entity.UserStatus;
import org.example.cloudopsadmin.entity.Permission;
import org.example.cloudopsadmin.repository.AccountRepository;
import org.example.cloudopsadmin.repository.PermissionRepository;
import org.example.cloudopsadmin.repository.RoleRepository;
import org.example.cloudopsadmin.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DataInitializer {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PermissionRepository permissionRepository, AccountRepository accountRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE credit_cards DROP COLUMN card_number");
            System.out.println("Dropped column 'card_number' from 'credit_cards' table.");
        } catch (Exception e) {
            // Column might not exist or other error, safe to ignore if we just want to ensure it's gone
            System.out.println("Could not drop column 'card_number' (might not exist): " + e.getMessage());
        }

        try {
            int deleted = jdbcTemplate.update("DELETE FROM cloud_providers WHERE name = ?", "Customer Account");
            if (deleted > 0) {
                System.out.println("Deleted 'Customer Account' from cloud_providers.");
            }
        } catch (Exception e) {
            System.out.println("Could not delete 'Customer Account' from cloud_providers: " + e.getMessage());
        }

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

        // Initialize UI specific permissions
        String[] permissionNames = {"USER_VIEW", "USER_EDIT", "SYSTEM_MANAGE", "LOG_VIEW"};
        String[] permissionDescs = {"查看用户", "编辑用户", "系统管理", "查看日志"};
        
        for (int i = 0; i < permissionNames.length; i++) {
            String name = permissionNames[i];
            String desc = permissionDescs[i];
            permissionRepository.findByName(name).orElseGet(() -> {
                Permission p = new Permission();
                p.setName(name);
                p.setDescription(desc);
                return permissionRepository.save(p);
            });
        }

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

        migrateLegacyAccountData();
    }

    private void migrateLegacyAccountData() {
        try {
            List<Account> accounts = accountRepository.findAll();
            for (Account account : accounts) {
                boolean changed = false;
                // Case 1: accountType is "Customer Account" (Old Vendor field value)
                if ("Customer Account".equals(account.getAccountType())) {
                    // Move "Customer Account" to category if null
                    if (account.getAccountCategory() == null) {
                        account.setAccountCategory("Customer Account");
                    }
                    
                    // Infer vendor. If UID is 12 digits, it's AWS.
                    String vendor = "AWS"; 
                    
                    account.setAccountType(vendor);
                    changed = true;
                }
                
                if (changed) {
                    accountRepository.save(account);
                    System.out.println("Migrated legacy account: " + account.getAccountInternalId());
                }
            }
        } catch (Exception e) {
            System.out.println("Error migrating legacy account data: " + e.getMessage());
        }
    }
}