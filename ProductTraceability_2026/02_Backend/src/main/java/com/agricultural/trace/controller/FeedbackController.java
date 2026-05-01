package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.service.FeedbackService;
import com.agricultural.trace.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户反馈控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtUtils jwtUtils;

    /**
     * 提交反馈
     */
    @PostMapping("/submit")
    public Result<Void> submit(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> body) {
        try {
            String content = body.get("content");
            if (content == null || content.trim().length() < 10) {
                return Result.error("反馈内容至少10个字");
            }

            Long userId = null;
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtils.getUserIdFromToken(token.substring(7));
                } catch (Exception ignored) {
                }
            }

            feedbackService.submitFeedback(userId, content.trim());
            return Result.success();
        } catch (Exception e) {
            log.error("提交反馈失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
