package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String method;

    private String params;

    private String result;

    private String ip;

    private String location;

    private String browser;

    private String os;

    private Integer status;

    private String errorMsg;

    private Integer executeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
