package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.service.S3StorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "文件上传接口")
public class FileController {

    private final S3StorageService s3StorageService;

    @PostMapping("/images/upload")
    @Operation(summary = "上传图片到S3", description = "上传图片文件到 S3 并返回访问 URL")
    public ApiResponse<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "folder", required = false) String folder
    ) {
        try {
            Map<String, Object> data = s3StorageService.uploadImage(file, folder);
            return ApiResponse.success("上传成功", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }
}

