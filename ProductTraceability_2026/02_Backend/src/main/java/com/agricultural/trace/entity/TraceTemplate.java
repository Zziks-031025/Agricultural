package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("trace_template")
public class TraceTemplate implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    private Integer templateType;

    private String productCategory;

    private String description;

    private String configJson;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
