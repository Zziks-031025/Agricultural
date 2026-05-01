package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_banner")
public class SysBanner implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private String title;

    private String description;

    private String imageUrl;

    private String linkUrl;

    private Integer targetType;

    private Integer sortOrder;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;
}
