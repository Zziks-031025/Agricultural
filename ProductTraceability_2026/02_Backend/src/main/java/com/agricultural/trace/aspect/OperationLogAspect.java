package com.agricultural.trace.aspect;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.entity.SysOperationLog;
import com.agricultural.trace.mapper.SysOperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.agricultural.trace.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
        
        SysOperationLog operationLog = new SysOperationLog();
        
        if (!annotation.module().isEmpty()) {
            operationLog.setModule(annotation.module());
            operationLog.setOperation(annotation.operation());
        } else {
            operationLog.setOperation(annotation.value());
        }
        
        operationLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        
        if (request != null) {
            operationLog.setIp(getIpAddress(request));
            operationLog.setLocation(getLocation(operationLog.getIp()));
            
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String username = extractUsernameFromToken(token.substring(7));
                    operationLog.setUsername(username);
                } catch (Exception e) {
                    operationLog.setUsername("unknown");
                }
            } else {
                operationLog.setUsername("anonymous");
            }
            
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    Map<String, Object> params = new HashMap<>();
                    String[] paramNames = signature.getParameterNames();
                    for (int i = 0; i < args.length && i < paramNames.length; i++) {
                        if (args[i] != null && !isFilterType(args[i])) {
                            params.put(paramNames[i], args[i]);
                        }
                    }
                    operationLog.setParams(objectMapper.writeValueAsString(params));
                }
            } catch (Exception e) {
                operationLog.setParams("参数解析失败");
            }
        }
        
        Object result = null;
        try {
            result = joinPoint.proceed();
            operationLog.setStatus(1);
            
            try {
                if (result != null) {
                    String resultStr = objectMapper.writeValueAsString(result);
                    if (resultStr.length() > 2000) {
                        resultStr = resultStr.substring(0, 2000) + "...";
                    }
                    operationLog.setResult(resultStr);
                }
            } catch (Exception e) {
                operationLog.setResult("结果序列化失败");
            }
            
        } catch (Exception e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            operationLog.setExecuteTime((int) (endTime - startTime));
            operationLog.setCreateTime(LocalDateTime.now());
            
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败: {}", e.getMessage());
            }
        }
        
        return result;
    }
    
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
    
    private String getLocation(String ip) {
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return "本地";
        }
        return "未知";
    }
    
    private String extractUsernameFromToken(String token) {
        return "admin";
    }
    
    private boolean isFilterType(Object arg) {
        return arg instanceof HttpServletRequest 
            || arg instanceof javax.servlet.http.HttpServletResponse
            || arg instanceof org.springframework.web.multipart.MultipartFile;
    }
}
