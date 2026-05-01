package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("trace_field")
public class TraceField implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long stageId;

    private String fieldCode;

    private String fieldName;

    private String fieldType;

    private String fieldOptions;

    private Integer isRequired;

    private Integer sort;

    private String placeholder;

    private String defaultValue;

    private String validationRule;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
