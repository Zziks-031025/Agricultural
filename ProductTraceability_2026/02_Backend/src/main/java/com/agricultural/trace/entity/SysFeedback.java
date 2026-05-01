package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈实体
 */
@Data
@TableName("sys_feedback")
public class SysFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String content;

    /**
     * 处理状态: 0-待处理, 1-已处理
     */
    private Integer status;

    private String reply;

    private LocalDateTime replyTime;

    private Long replyBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
