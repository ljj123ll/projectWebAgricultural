package com.agricultural.assistplatform.controller.common;

import com.agricultural.assistplatform.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/common/upload")
@Tag(name = "文件上传模块")
public class FileUploadController {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    @Value("${app.upload.max-size:10485760}")
    private long maxSize;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadPath);
        try {
            Files.createDirectories(rootLocation);
            // 创建子目录
            Files.createDirectories(rootLocation.resolve("products"));
            Files.createDirectories(rootLocation.resolve("avatars"));
            Files.createDirectories(rootLocation.resolve("qualifications"));
            Files.createDirectories(rootLocation.resolve("news"));
            log.info("文件上传目录初始化完成: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("无法创建上传目录", e);
            throw new RuntimeException("无法创建上传目录", e);
        }
    }

    /**
     * 通用图片上传
     */
    @PostMapping("/image")
    @Operation(summary = "通用图片上传")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String type) {
        return uploadFile(file, type);
    }

    /**
     * 商品图片上传
     */
    @PostMapping("/product")
    @Operation(summary = "商品图片上传")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "products");
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    @Operation(summary = "头像上传")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "avatars");
    }

    /**
     * 资质图片上传
     */
    @PostMapping("/qualification")
    @Operation(summary = "资质图片上传")
    public Result<Map<String, String>> uploadQualification(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "qualifications");
    }

    /**
     * 新闻图片上传
     */
    @PostMapping("/news")
    @Operation(summary = "新闻图片上传")
    public Result<Map<String, String>> uploadNewsImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "news");
    }

    /**
     * 多文件上传
     */
    @PostMapping("/batch")
    @Operation(summary = "批量图片上传")
    public Result<Map<String, Object>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(defaultValue = "general") String type) {
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String> successFiles = new HashMap<>();
        Map<String, String> failedFiles = new HashMap<>();

        for (MultipartFile file : files) {
            try {
                Result<Map<String, String>> uploadResult = uploadFile(file, type);
                if (uploadResult.getCode() == 200) {
                    successFiles.put(file.getOriginalFilename(), uploadResult.getData().get("url"));
                } else {
                    failedFiles.put(file.getOriginalFilename(), uploadResult.getMsg());
                }
            } catch (Exception e) {
                failedFiles.put(file.getOriginalFilename(), e.getMessage());
            }
        }

        result.put("success", successFiles);
        result.put("failed", failedFiles);
        result.put("total", files.length);
        result.put("successCount", successFiles.size());
        result.put("failedCount", failedFiles.size());

        return Result.ok(result);
    }

    private Result<Map<String, String>> uploadFile(MultipartFile file, String type) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return Result.fail(400, "请选择要上传的文件");
        }

        // 检查文件大小
        if (file.getSize() > maxSize) {
            return Result.fail(400, "文件大小超过限制，最大允许" + (maxSize / 1024 / 1024) + "MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            return Result.fail(400, "不支持的文件类型，仅允许: " + allowedTypes);
        }

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path destinationFile = rootLocation.resolve(type).resolve(filename);
            Files.copy(file.getInputStream(), destinationFile);

            // 构建访问URL
            String fileUrl = "/uploads/" + type + "/" + filename;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", filename);
            result.put("originalName", originalFilename);
            result.put("size", String.valueOf(file.getSize()));

            log.info("文件上传成功: {}", fileUrl);
            return Result.ok(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }

    private boolean isAllowedType(String contentType) {
        return Arrays.asList(allowedTypes.split(",")).contains(contentType);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
