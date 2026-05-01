package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.agricultural.trace.config.FileUploadConfig;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {

    public FileController(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }

    private final FileUploadConfig fileUploadConfig;

    /**
     * 通用文件上传
     * @param file 上传的文件
     * @param type 文件类型分类 (如 license / certificate / record)
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "common") String type
    ) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 文件大小校验 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        try {
            // 按日期+类型分目录存储
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String subDir = type + "/" + dateDir;
            File targetDir = new File(fileUploadConfig.getUploadDir(), subDir);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件
            File dest = new File(targetDir, newFileName);
            file.transferTo(dest);

            // 返回访问 URL
            String url = "/uploads/" + subDir + "/" + newFileName;
            log.info("文件上传成功: {} -> {}", originalName, url);

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("name", originalName);
            return Result.success("上传成功", data);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
