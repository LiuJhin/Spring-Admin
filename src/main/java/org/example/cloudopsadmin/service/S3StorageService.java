package org.example.cloudopsadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:${AWS_S3_BUCKET:}}")
    private String bucket;

    @Value("${aws.region:${AWS_REGION:us-east-1}}")
    private String region;

    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("仅支持上传图片文件");
        }
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("未配置 S3 bucket");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String baseFolder = StringUtils.hasText(folder) ? folder.trim() : "images";
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image";
        String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = baseFolder + "/" + datePath + "/" + UUID.randomUUID() + "_" + sanitized;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectResponse response = s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

        Map<String, Object> result = new HashMap<>();
        result.put("bucket", bucket);
        result.put("key", key);
        result.put("url", url);
        result.put("etag", response.eTag());
        result.put("size", file.getSize());
        result.put("content_type", contentType);
        return result;
    }
}

