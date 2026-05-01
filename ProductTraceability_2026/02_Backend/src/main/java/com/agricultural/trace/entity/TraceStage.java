package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("trace_stage")
public class TraceStage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String stageCode;

    private String stageName;

    private Integer stageType;

    private Integer sort;

    private Integer isRequired;

    private String description;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
