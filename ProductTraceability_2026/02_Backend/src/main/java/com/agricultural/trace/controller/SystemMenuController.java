package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.SysUserMapper;
import com.agricultural.trace.service.SystemMenuService;
import com.agricultural.trace.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
@CrossOrigin
public class SystemMenuController {

    private final SystemMenuService systemMenuService;
    private final SysUserMapper sysUserMapper;
    private final JwtUtils jwtUtils;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree(@RequestHeader("Authorization") String token) {
        try {
            String rawToken = token;
            if (rawToken != null && rawToken.startsWith("Bearer ")) {
                rawToken = rawToken.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(rawToken);
            SysUser user = sysUserMapper.selectById(userId);
            
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            List<Map<String, Object>> menuTree = systemMenuService.getUserMenuTree(
                user.getUserType(), 
                user.getEnterpriseType()
            );
            return Result.success(menuTree);
        } catch (Exception e) {
            log.error("获取菜单树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
