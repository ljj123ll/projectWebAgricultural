package com.agricultural.assistplatform.controller.common;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    // Set upload directory to project root/uploads
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        try {
            // Create directory if not exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;
            
            // Save file
            File dest = new File(UPLOAD_DIR + fileName);
            file.transferTo(dest);

            // Return accessible URL path
            // Assuming static resource mapping maps /uploads/** to file:./uploads/
            return Result.ok("/uploads/" + fileName);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件上传失败");
        }
    }
}
