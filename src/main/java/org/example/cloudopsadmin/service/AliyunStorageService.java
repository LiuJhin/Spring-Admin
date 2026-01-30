package org.example.cloudopsadmin.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AliyunStorageService {

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("仅支持上传图片文件");
        }
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("未配置 OSS bucket");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String baseFolder = StringUtils.hasText(folder) ? folder.trim() : "images";
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image";
        String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = baseFolder + "/" + datePath + "/" + UUID.randomUUID() + "_" + sanitized;

        PutObjectRequest request = new PutObjectRequest(bucketName, key, file.getInputStream());
        // Configure content type if needed, though OSS usually detects it or it can be set in metadata
        // ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setContentType(contentType);
        // request.setMetadata(metadata);
        
        // OSS client putObject returns PutObjectResult
        PutObjectResult result = ossClient.putObject(request);

        // Return Key instead of full URL for database storage
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("bucket", bucketName);
        responseMap.put("key", key);
        responseMap.put("etag", result.getETag());
        responseMap.put("size", file.getSize());
        responseMap.put("content_type", contentType);
        
        // Generate a presigned URL for immediate display
        responseMap.put("url", generatePresignedUrl(key));
        
        return responseMap;
    }

    public String generatePresignedUrl(String key) {
        if (!StringUtils.hasText(key)) return null;
        
        // 构造本站 OSS 的基础 URL (支持 https 和 http)
        String ossHostHttps = "https://" + bucketName + "." + endpoint + "/";
        String ossHostHttp = "http://" + bucketName + "." + endpoint + "/";

        // 如果是本站完整 URL (旧数据)，尝试提取 Key
        if (key.startsWith(ossHostHttps)) {
            key = key.substring(ossHostHttps.length());
        } else if (key.startsWith(ossHostHttp)) {
            key = key.substring(ossHostHttp.length());
        } else if (key.startsWith("http")) {
             // 是 http(s) 开头，但不是本站当前配置的 URL (可能是外部链接，或旧配置)，直接返回
             return key;
        }
        
        // 去除可能存在的 query params
        if (key.contains("?")) {
            key = key.substring(0, key.indexOf("?"));
        }

        // 生成签名 URL，默认过期时间 1 小时
        java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3600 * 1000);
        java.net.URL url = ossClient.generatePresignedUrl(bucketName, key, expiration);
        return url.toString();
    }
}
