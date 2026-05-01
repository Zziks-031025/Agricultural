package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysMessage;
import com.agricultural.trace.service.MessageService;
import com.agricultural.trace.utils.JwtUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@CrossOrigin
public class MessageController {

    private final MessageService messageService;
    private final JwtUtils jwtUtils;

    /**
     * 消息列表（分页）
     */
    @GetMapping("/list")
    public Result<Page<SysMessage>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String type) {
        try {
            Long userId = extractUserId(token);
            Page<SysMessage> page = messageService.getMessageList(userId, type, current, size);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取消息列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 消息详情
     */
    @GetMapping("/detail/{id}")
    public Result<SysMessage> detail(@PathVariable Long id) {
        try {
            SysMessage message = messageService.getMessageDetail(id);
            if (message == null) {
                return Result.error("消息不存在");
            }
            return Result.success(message);
        } catch (Exception e) {
            log.error("获取消息详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserId(token);
            Map<String, Long> counts = messageService.getUnreadCount(userId);
            return Result.success(counts);
        } catch (Exception e) {
            log.error("获取未读数量失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量标记已读
     */
    @OperationLog(module = "消息管理", operation = "标记消息已读")
    @SuppressWarnings("unchecked")
    @PostMapping("/mark-read")
    public Result<Void> markRead(@RequestBody Map<String, Object> body) {
        try {
            List<?> ids = (List<?>) body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("消息ID不能为空");
            }
            List<Long> longIds = parseIds(ids);
            messageService.markAsRead(longIds);
            return Result.success();
        } catch (Exception e) {
            log.error("标记已读失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除消息
     */
    @OperationLog(module = "消息管理", operation = "删除消息")
    @SuppressWarnings("unchecked")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        try {
            List<?> ids = (List<?>) body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("消息ID不能为空");
            }
            List<Long> longIds = parseIds(ids);
            messageService.deleteMessages(longIds);
            return Result.success();
        } catch (Exception e) {
            log.error("删除消息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    private List<Long> parseIds(List<?> ids) {
        return ids.stream().map(item -> {
            if (item instanceof Number) {
                return ((Number) item).longValue();
            }
            return Long.parseLong(item.toString());
        }).collect(Collectors.toList());
    }

    private Long extractUserId(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }
}
