package org.example.cloudopsadmin.controller;

import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.User;
import org.example.cloudopsadmin.repository.UserRepository;
import org.example.cloudopsadmin.service.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String token = jwtService.generateAccessToken(user);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", "u_" + user.getId());
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("role", user.getRoles().isEmpty() ? "user" : user.getRoles().get(0).getName().toLowerCase());
        userData.put("status", user.getStatus().toString().toLowerCase());
        userData.put("avatar", user.getAvatar());
        userData.put("createdAt", user.getCreatedAt());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userData);

        return ApiResponse.success("登录成功", data);
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> data = new HashMap<>();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.blacklistToken(token);
        }
        return ApiResponse.success("退出登录成功", data);
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
