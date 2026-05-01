package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysMenu;
import com.agricultural.trace.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMenuService {

    private final SysMenuMapper sysMenuMapper;

    public List<Map<String, Object>> getUserMenuTree(Integer userType, Integer enterpriseType) {
        List<SysMenu> menus = sysMenuMapper.selectMenusByUserTypeAndEnterpriseType(userType, enterpriseType);
        return buildMenuTree(menus, 0L);
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", menu.getId());
                node.put("menuName", menu.getMenuName());
                node.put("menuCode", menu.getMenuCode());
                node.put("menuType", menu.getMenuType());
                node.put("path", menu.getPath());
                node.put("component", menu.getComponent());
                node.put("perms", menu.getPerms());
                node.put("icon", menu.getIcon());
                node.put("visible", menu.getVisible());
                
                List<Map<String, Object>> children = buildMenuTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                tree.add(node);
            }
        }
        return tree;
    }
}
