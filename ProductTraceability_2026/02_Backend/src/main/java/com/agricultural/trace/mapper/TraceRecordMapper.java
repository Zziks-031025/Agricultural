package com.agricultural.trace.mapper;

import com.agricultural.trace.entity.TraceRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 生长记录表 Mapper
 */
@Mapper
public interface TraceRecordMapper extends BaseMapper<TraceRecord> {

    /**
     * 软删除：标记deleted=1并记录delete_time（绕过@TableLogic）
     */
    @Update("UPDATE trace_record SET deleted = 1, delete_time = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(Long id);

    /**
     * 查询已软删除且超过指定天数的记录（绕过@TableLogic）
     */
    @Select("SELECT * FROM trace_record WHERE deleted = 1 AND delete_time <= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<TraceRecord> selectDeletedOlderThan(int days);

    /**
     * 物理删除单条记录（绕过@TableLogic）
     */
    @Delete("DELETE FROM trace_record WHERE id = #{id} AND deleted = 1")
    int physicalDeleteById(Long id);
}
