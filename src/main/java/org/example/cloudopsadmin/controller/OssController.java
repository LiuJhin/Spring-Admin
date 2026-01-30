package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.service.AliyunStorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oss")
@RequiredArgsConstructor
@Tag(name = "OSS Management", description = "对象存储管理接口")
public class OssController {

    private final AliyunStorageService storageService;

    @GetMapping("/url")
    @Operation(summary = "获取文件签名 URL", description = "根据文件 Key 获取临时的签名访问 URL (有效期 1 小时)")
    public ApiResponse<Map<String, String>> getPresignedUrl(@RequestParam("key") String key) {
        String url = storageService.generatePresignedUrl(key);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        return ApiResponse.success("获取签名 URL 成功", response);
    }
}
