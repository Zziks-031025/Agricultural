package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_article")
public class EduArticle implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private String category;

    private String title;

    private String author;

    private String summary;

    private String coverUrl;

    private String content;

    private Integer viewCount;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;
}
