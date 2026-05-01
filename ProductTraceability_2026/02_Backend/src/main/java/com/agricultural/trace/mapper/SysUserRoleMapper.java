package com.agricultural.trace.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 插入用户角色关联
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (#{userId}, #{roleId}, NOW())")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
