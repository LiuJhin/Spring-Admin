package org.example.cloudopsadmin.controller;

import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getUserList() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", "u_" + user.getId());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("role", user.getRoles().isEmpty() ? "user" : user.getRoles().get(0).getName().toLowerCase());
            userData.put("status", user.getStatus().toString().toLowerCase());
            userData.put("avatar", user.getAvatar());
            userData.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            return userData;
        }).collect(Collectors.toList());

        return ApiResponse.success("获取用户列表成功", userList);
    }
}
