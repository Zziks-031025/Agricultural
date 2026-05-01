package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysRole;
import com.agricultural.trace.service.SystemRoleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
@CrossOrigin
public class SystemRoleController {

    private final SystemRoleService systemRoleService;

    @GetMapping("/list")
    public Result<Page<SysRole>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roleName) {
        try {
            Page<SysRole> page = systemRoleService.getRoleList(current, size, roleName);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询角色列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        try {
            return Result.success(systemRoleService.getAllRoles());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "角色管理", operation = "新增角色")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysRole role) {
        try {
            systemRoleService.addRole(role);
            return Result.success();
        } catch (Exception e) {
            log.error("新增角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "角色管理", operation = "更新角色")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysRole role) {
        try {
            systemRoleService.updateRole(role);
            return Result.success();
        } catch (Exception e) {
            log.error("更新角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "角色管理", operation = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            systemRoleService.deleteRole(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/menus/{roleId}")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        try {
            return Result.success(systemRoleService.getRoleMenuIds(roleId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "角色管理", operation = "分配菜单权限")
    @SuppressWarnings("unchecked")
    @PutMapping("/menus/{roleId}")
    public Result<Void> saveRoleMenus(@PathVariable Long roleId, @RequestBody Map<String, Object> body) {
        try {
            List<Number> menuIdNumbers = (List<Number>) body.get("menuIds");
            List<Long> menuIds = menuIdNumbers != null
                    ? menuIdNumbers.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList())
                    : java.util.Collections.emptyList();
            systemRoleService.saveRoleMenus(roleId, menuIds);
            return Result.success();
        } catch (Exception e) {
            log.error("保存角色菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
