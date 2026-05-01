package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysMenu;
import com.agricultural.trace.entity.SysRole;
import com.agricultural.trace.mapper.SysMenuMapper;
import com.agricultural.trace.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    public Page<SysRole> getRoleList(Integer current, Integer size, String roleName) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        wrapper.orderByAsc(SysRole::getSort);
        return sysRoleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SysRole> getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        wrapper.orderByAsc(SysRole::getSort);
        return sysRoleMapper.selectList(wrapper);
    }

    public void addRole(SysRole role) {
        LambdaQueryWrapper<SysRole> check = new LambdaQueryWrapper<>();
        check.eq(SysRole::getRoleCode, role.getRoleCode());
        if (sysRoleMapper.selectCount(check) > 0) {
            throw new RuntimeException("角色编码已存在");
        }
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.insert(role);
        log.info("新增角色成功, roleCode={}", role.getRoleCode());
    }

    public void updateRole(SysRole role) {
        SysRole existing = sysRoleMapper.selectById(role.getId());
        if (existing == null) {
            throw new RuntimeException("角色不存在");
        }
        existing.setRoleName(role.getRoleName());
        existing.setRoleType(role.getRoleType());
        existing.setSort(role.getSort());
        existing.setStatus(role.getStatus());
        existing.setRemark(role.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.updateById(existing);
        log.info("更新角色成功, id={}", role.getId());
    }

    public void deleteRole(Long id) {
        throw new RuntimeException("角色管理功能已简化，请直接管理用户类型");
    }

    public List<Long> getRoleMenuIds(Long roleId) {
        return new ArrayList<>();
    }

    @Transactional
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        throw new RuntimeException("角色管理功能已简化，菜单权限由用户类型和企业类型自动控制");
    }

    public List<Map<String, Object>> getMenuTree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus, 1);
        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = sysMenuMapper.selectList(wrapper);
        return buildTree(allMenus, 0L);
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> menus, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", menu.getId());
                node.put("label", menu.getMenuName());
                node.put("menuCode", menu.getMenuCode());
                node.put("menuType", menu.getMenuType());
                node.put("path", menu.getPath());
                node.put("component", menu.getComponent());
                node.put("perms", menu.getPerms());
                node.put("icon", menu.getIcon());
                List<Map<String, Object>> children = buildTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                tree.add(node);
            }
        }
        return tree;
    }
}
