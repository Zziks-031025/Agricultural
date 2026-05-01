package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysFeedback;
import com.agricultural.trace.mapper.SysFeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final SysFeedbackMapper sysFeedbackMapper;

    /**
     * 提交反馈
     */
    public void submitFeedback(Long userId, String content) {
        SysFeedback feedback = new SysFeedback();
        feedback.setUserId(userId);
        feedback.setContent(content);
        feedback.setStatus(0);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        sysFeedbackMapper.insert(feedback);
        log.info("用户反馈已提交, userId={}", userId);
    }
}
