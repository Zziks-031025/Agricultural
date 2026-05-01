package com.agricultural.trace.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * 文件清理工具类
 * 负责删除服务器上不再需要的物理文件
 */
@Slf4j
public class FileCleanupUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 删除单个文件（根据相对路径）
     * @param uploadDir 上传根目录的绝对路径
     * @param relativePath 文件相对路径，如 /uploads/record/20260301/xxx.jpg
     * @return 是否成功删除
     */
    public static boolean deleteFile(String uploadDir, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        // 去掉 /uploads/ 前缀，得到子路径
        String subPath = relativePath;
        if (subPath.startsWith("/uploads/")) {
            subPath = subPath.substring("/uploads/".length());
        } else if (subPath.startsWith("uploads/")) {
            subPath = subPath.substring("uploads/".length());
        }

        File file = new File(uploadDir, subPath);
        if (file.exists() && file.isFile()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("已删除物理文件: {}", file.getAbsolutePath());
            } else {
                log.warn("删除物理文件失败: {}", file.getAbsolutePath());
            }
            return deleted;
        } else {
            log.debug("文件不存在，跳过删除: {}", file.getAbsolutePath());
            return false;
        }
    }

    /**
     * 删除多个文件（根据JSON数组格式的images字段）
     * @param uploadDir 上传根目录的绝对路径
     * @param imagesJson JSON数组格式的图片路径，如 ["/uploads/record/xxx.jpg"]
     */
    public static void deleteImages(String uploadDir, String imagesJson) {
        List<String> paths = parseImagePaths(imagesJson);
        for (String path : paths) {
            deleteFile(uploadDir, path);
        }
    }

    /**
     * 解析JSON数组格式的图片路径
     * @param imagesJson JSON数组字符串
     * @return 路径列表
     */
    public static List<String> parseImagePaths(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty() || "[]".equals(imagesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析images JSON失败: {}", imagesJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 比较新旧路径，删除被替换掉的旧文件
     * 用于营业执照、封面图等更新覆盖场景
     * @param uploadDir 上传根目录
     * @param oldPath 旧文件路径
     * @param newPath 新文件路径
     */
    public static void deleteIfChanged(String uploadDir, String oldPath, String newPath) {
        if (oldPath == null || oldPath.isEmpty()) {
            return;
        }
        // 路径不同，说明文件被替换了
        if (!oldPath.equals(newPath)) {
            deleteFile(uploadDir, oldPath);
        }
    }
}
