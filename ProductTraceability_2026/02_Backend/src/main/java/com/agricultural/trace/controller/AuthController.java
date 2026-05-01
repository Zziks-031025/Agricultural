package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.dto.LoginDTO;
import com.agricultural.trace.service.AuthService;
import com.agricultural.trace.service.LoginLogService;
import com.agricultural.trace.utils.JwtUtils;
import com.agricultural.trace.vo.LoginVO;
import com.agricultural.trace.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LoginLogService loginLogService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        try {
            LoginVO loginVO = authService.login(loginDTO);
            loginLogService.recordSuccess(loginVO, loginDTO.getUsername(), ip, userAgent);
            return Result.success(loginVO);
        } catch (RuntimeException e) {
            loginLogService.recordFailure(loginDTO.getUsername(), ip, userAgent, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> body) {
        try {
            authService.registerOrdinaryUser(body);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Long userId = jwtUtils.getUserIdFromToken(token);
            UserInfoVO userInfo = authService.getUserInfo(userId);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(401, "鉴权失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
