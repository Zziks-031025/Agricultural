package com.agricultural.trace.mapper;

import com.agricultural.trace.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT * FROM sys_menu WHERE status = 1 " +
            "AND (user_types IS NULL OR FIND_IN_SET(#{userType}, user_types) > 0) " +
            "AND (enterprise_types IS NULL OR #{enterpriseType} IS NULL OR FIND_IN_SET(#{enterpriseType}, enterprise_types) > 0) " +
            "ORDER BY sort ASC")
    List<SysMenu> selectMenusByUserTypeAndEnterpriseType(@Param("userType") Integer userType, 
                                                          @Param("enterpriseType") Integer enterpriseType);
}
