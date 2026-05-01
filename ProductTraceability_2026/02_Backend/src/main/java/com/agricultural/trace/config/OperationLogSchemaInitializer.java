package com.agricultural.trace.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogSchemaInitializer implements ApplicationRunner {

    private static final String COLUMN_CHECK_SQL =
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
    private static final String ADD_MODULE_COLUMN_SQL =
            "ALTER TABLE sys_operation_log ADD COLUMN module VARCHAR(50) COMMENT '操作模块' AFTER username";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureModuleColumn();
    }

    void ensureModuleColumn() {
        Integer columnCount = jdbcTemplate.queryForObject(
                COLUMN_CHECK_SQL,
                Integer.class,
                "sys_operation_log",
                "module"
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(ADD_MODULE_COLUMN_SQL);
            log.info("Added missing sys_operation_log.module column for backward compatibility");
        }
    }
}
