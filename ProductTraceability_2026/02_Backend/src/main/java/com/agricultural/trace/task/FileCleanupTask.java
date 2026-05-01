package com.agricultural.trace.task;

import com.agricultural.trace.config.FileUploadConfig;
import com.agricultural.trace.entity.TraceRecord;
import com.agricultural.trace.service.RecordService;
import com.agricultural.trace.util.FileCleanupUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件清理定时任务
 * 每天凌晨3点执行，清理已软删除超过7天的生长记录关联的物理文件，并物理删除数据库记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    private final RecordService recordService;
    private final FileUploadConfig fileUploadConfig;

    private static final int RETENTION_DAYS = 7;

    /**
     * 每天凌晨3:00执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupDeletedRecordFiles() {
        log.info("开始执行文件清理任务，清理已删除超过{}天的记录文件...", RETENTION_DAYS);

        try {
            List<TraceRecord> records = recordService.findDeletedRecordsOlderThan(RETENTION_DAYS);

            if (records.isEmpty()) {
                log.info("文件清理任务完成，无需清理的记录");
                return;
            }

            int fileCount = 0;
            int recordCount = 0;
            String uploadDir = fileUploadConfig.getUploadDir();

            for (TraceRecord record : records) {
                // 删除关联的物理文件
                if (record.getImages() != null && !record.getImages().isEmpty()) {
                    List<String> paths = FileCleanupUtil.parseImagePaths(record.getImages());
                    for (String path : paths) {
                        if (FileCleanupUtil.deleteFile(uploadDir, path)) {
                            fileCount++;
                        }
                    }
                }
                // 物理删除数据库记录
                recordService.physicalDelete(record.getId());
                recordCount++;
            }

            log.info("文件清理任务完成，清理了{}条记录，删除了{}个文件", recordCount, fileCount);
        } catch (Exception e) {
            log.error("文件清理任务异常", e);
        }
    }
}
