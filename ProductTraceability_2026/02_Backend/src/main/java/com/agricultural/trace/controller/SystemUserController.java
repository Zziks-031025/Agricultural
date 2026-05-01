package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.service.SystemUserService;
import com.agricultural.trace.utils.JwtUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
@CrossOrigin
public class SystemUserController {

    private final SystemUserService systemUserService;
    private final JwtUtils jwtUtils;

    @GetMapping("/list")
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status) {
        try {
            Page<SysUser> page = systemUserService.getUserList(current, size, username, phone, keyword, userType, status);
            page.getRecords().forEach(user -> user.setPassword(null));
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询用户列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "用户管理", operation = "新增用户")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        try {
            SysUser user = new SysUser();
            user.setUsername((String) body.get("username"));
            user.setRealName((String) body.get("realName"));
            user.setPhone((String) body.get("phone"));
            user.setEmail((String) body.get("email"));
            user.setUserType(parseInteger(body.get("userType"), 2));
            user.setEnterpriseId(parseLong(body.get("enterpriseId")));
            user.setRemark((String) body.get("remark"));
            user.setStatus(1);

            String password = (String) body.get("password");
            systemUserService.addUser(user, password, parseRoleIds(body));
            return Result.success();
        } catch (Exception e) {
            log.error("新增用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "用户管理", operation = "更新用户")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        try {
            SysUser user = new SysUser();
            user.setId(parseRequiredLong(body.get("id"), "用户ID不能为空"));
            user.setRealName((String) body.get("realName"));
            user.setPhone((String) body.get("phone"));
            user.setEmail((String) body.get("email"));
            user.setAvatar((String) body.get("avatar"));
            user.setUserType(parseInteger(body.get("userType"), null));
            user.setEnterpriseId(parseLong(body.get("enterpriseId")));
            user.setRemark((String) body.get("remark"));

            systemUserService.updateUser(user, parseRoleIds(body));
            return Result.success();
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "用户管理", operation = "删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            systemUserService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "用户管理", operation = "重置密码")
    @PutMapping("/reset-password/{id}")
    public Result<String> resetPassword(@PathVariable Long id) {
        try {
            return Result.success(systemUserService.resetPassword(id));
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "用户管理", operation = "切换用户状态")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            systemUserService.toggleStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("切换用户状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        try {
            String rawToken = token;
            if (rawToken != null && rawToken.startsWith("Bearer ")) {
                rawToken = rawToken.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(rawToken);

            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return Result.error("密码不能为空");
            }
            systemUserService.changePassword(userId, oldPassword, newPassword);
            return Result.success();
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    private List<Long> parseRoleIds(Map<String, Object> body) {
        List<Long> roleIds = new ArrayList<>();
        Object roleIdsObj = body.get("roleIds");
        if (roleIdsObj instanceof List) {
            for (Object item : (List<?>) roleIdsObj) {
                Long roleId = parseLong(item);
                if (roleId != null) {
                    roleIds.add(roleId);
                }
            }
        }

        if (roleIds.isEmpty()) {
            Long roleId = parseLong(body.get("roleId"));
            if (roleId != null) {
                roleIds.add(roleId);
            }
        }
        return roleIds;
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : Integer.parseInt(text);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : Long.parseLong(text);
    }

    private Long parseRequiredLong(Object value, String errorMessage) {
        Long parsed = parseLong(value);
        if (parsed == null) {
            throw new RuntimeException(errorMessage);
        }
        return parsed;
    }
}
