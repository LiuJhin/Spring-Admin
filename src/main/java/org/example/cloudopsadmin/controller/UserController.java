package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Permission;
import org.example.cloudopsadmin.entity.Role;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.entity.UserStatus;
import org.example.cloudopsadmin.repository.PermissionRepository;
import org.example.cloudopsadmin.repository.RoleRepository;
import org.example.cloudopsadmin.repository.UserRepository;
import org.example.cloudopsadmin.service.AliyunStorageService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "用户管理接口")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AliyunStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，包含角色和权限配置")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ApiResponse.error(400, "该邮箱已被注册");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Handle Status
        try {
            if ("正常".equals(request.getStatus()) || "active".equalsIgnoreCase(request.getStatus())) {
                user.setStatus(UserStatus.ACTIVE);
            } else if ("禁用".equals(request.getStatus()) || "inactive".equalsIgnoreCase(request.getStatus()) || "banned".equalsIgnoreCase(request.getStatus())) {
                user.setStatus(UserStatus.INACTIVE);
            } else {
                user.setStatus(UserStatus.ACTIVE);
            }
        } catch (Exception e) {
            user.setStatus(UserStatus.ACTIVE);
        }

        // Handle Role
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            String roleName = request.getRole();
            if ("管理员".equals(roleName)) {
                roleName = "ADMIN";
            }
            Role role = roleRepository.findByName(roleName);
            if (role != null) {
                user.setRoles(Collections.singletonList(role));
            }
        }

        // Handle Permissions
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            List<Permission> permissions = new ArrayList<>();
            Map<String, String> uiToDbMap = new HashMap<>();
            uiToDbMap.put("查看用户", "USER_VIEW");
            uiToDbMap.put("编辑用户", "USER_EDIT");
            uiToDbMap.put("系统管理", "SYSTEM_MANAGE");
            uiToDbMap.put("查看日志", "LOG_VIEW");

            for (String permInput : request.getPermissions()) {
                String dbName = uiToDbMap.getOrDefault(permInput, permInput);
                permissionRepository.findByName(dbName).ifPresent(permissions::add);
            }
            user.setPermissions(permissions);
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        return ApiResponse.success("用户创建成功", response);
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Operation(summary = "上传当前用户头像", description = "上传图片并更新当前登录用户的头像")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Map<String, Object> uploadResult = storageService.uploadImage(file, "avatars");
            String avatarUrl = (String) uploadResult.get("url");
            String avatarKey = (String) uploadResult.get("key");

            User userToUpdate = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalStateException("User not found"));
            userToUpdate.setAvatar(avatarKey);
            userRepository.save(userToUpdate);

            Map<String, Object> response = new HashMap<>();
            response.put("avatar", avatarUrl);
            response.put("upload_info", uploadResult);
            
            return ApiResponse.success("头像上传并更新成功", response);
        } catch (Throwable e) {
            e.printStackTrace();
            return ApiResponse.error(500, "头像上传失败: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "获取用户列表", description = "获取系统所有用户列表")
    public ApiResponse<List<Map<String, Object>>> getUserList() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", "u_" + user.getId());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("role", user.getRoles().isEmpty() ? "user" : user.getRoles().get(0).getName().toLowerCase());
            userData.put("status", user.getStatus().toString().toLowerCase());
            userData.put("avatar", storageService.generatePresignedUrl(user.getAvatar()));
            userData.put("createdAt", user.getCreatedAt());
            return userData;
        }).collect(Collectors.toList());

        return ApiResponse.success("获取用户列表成功", userList);
    }

    @Data
    public static class CreateUserRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private String status;
        private List<String> permissions;
    }
}
