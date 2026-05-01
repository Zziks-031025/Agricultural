package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息通知实体
 */
@Data
@TableName("sys_message")
public class SysMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 消息类型: system-系统通知, business-业务提醒
     */
    private String type;

    private String title;

    private String summary;

    private String content;

    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;

    private String actionUrl;

    private String actionTitle;

    private String actionDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
