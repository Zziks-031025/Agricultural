package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysLoginLog;
import com.agricultural.trace.mapper.SysLoginLogMapper;
import com.agricultural.trace.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    public void recordSuccess(LoginVO loginVO, String username, String ip, String userAgent) {
        SysLoginLog logEntity = baseLog(username, ip, userAgent);
        if (loginVO != null && loginVO.getUserInfo() != null) {
            logEntity.setUserId(loginVO.getUserInfo().getId());
            if (StringUtils.hasText(loginVO.getUserInfo().getUsername())) {
                logEntity.setUsername(loginVO.getUserInfo().getUsername());
            }
        }
        logEntity.setStatus(1);
        logEntity.setMessage("登录成功");
        sysLoginLogMapper.insert(logEntity);
    }

    public void recordFailure(String username, String ip, String userAgent, String message) {
        SysLoginLog logEntity = baseLog(username, ip, userAgent);
        logEntity.setStatus(0);
        logEntity.setMessage(StringUtils.hasText(message) ? message : "登录失败");
        sysLoginLogMapper.insert(logEntity);
    }

    private SysLoginLog baseLog(String username, String ip, String userAgent) {
        SysLoginLog logEntity = new SysLoginLog();
        logEntity.setUsername(username);
        logEntity.setIp(ip);
        logEntity.setBrowser(extractBrowser(userAgent));
        logEntity.setOs(extractOs(userAgent));
        logEntity.setLocation("");
        logEntity.setLoginTime(LocalDateTime.now());
        return logEntity;
    }

    private String extractBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            return "Safari";
        }
        return truncate(userAgent);
    }

    private String extractOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return truncate(userAgent);
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 100) {
            return value;
        }
        return value.substring(0, 100);
    }
}
