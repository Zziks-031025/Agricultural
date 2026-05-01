package com.agricultural.trace.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * 文件上传配置 - 静态资源映射
 */
@Slf4j
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String getUploadDir() {
        return uploadDir;
    }

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            // 基于应用JAR/classes所在位置定位后端根目录，确保部署环境下路径正确
            String basePath = resolveBasePath();
            dir = new File(basePath, uploadDir);
        }
        try {
            uploadDir = dir.getCanonicalPath();
        } catch (Exception e) {
            uploadDir = dir.getAbsolutePath();
        }
        dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("创建上传目录: {}", uploadDir);
        }
        log.info("文件上传目录(绝对路径): {}", uploadDir);
    }

    private String resolveBasePath() {
        try {
            // 获取当前类的class文件/jar包所在路径
            File classDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            // 开发环境: target/classes -> 向上两级到 02_Backend
            // 部署环境: xxx.jar 所在目录即为后端根目录
            if (classDir.isDirectory() && classDir.getPath().contains("target")) {
                return classDir.getParentFile().getParentFile().getAbsolutePath();
            }
            return classDir.getParentFile().getAbsolutePath();
        } catch (Exception e) {
            log.warn("无法通过ClassPath解析基础路径，回退到user.dir: {}", e.getMessage());
            return System.getProperty("user.dir");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        String absolutePath = dir.getAbsolutePath().replace("\\", "/");
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
        log.info("静态资源映射: /uploads/** -> file:{}", absolutePath);
    }
}
