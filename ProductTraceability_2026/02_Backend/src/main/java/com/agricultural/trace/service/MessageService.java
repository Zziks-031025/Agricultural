package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysMessage;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.SysMessageMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final SysMessageMapper sysMessageMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 分页查询消息列表
     */
    public Page<SysMessage> getMessageList(Long userId, String type, Integer current, Integer size) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId);
        if (StringUtils.hasText(type)) {
            wrapper.eq(SysMessage::getType, type);
        }
        wrapper.orderByDesc(SysMessage::getCreateTime);
        return sysMessageMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 获取消息详情
     */
    public SysMessage getMessageDetail(Long id) {
        return sysMessageMapper.selectById(id);
    }

    /**
     * 获取未读消息数量
     */
    public Map<String, Long> getUnreadCount(Long userId) {
        Map<String, Long> result = new HashMap<>();

        LambdaQueryWrapper<SysMessage> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(SysMessage::getUserId, userId).eq(SysMessage::getIsRead, 0);
        long allCount = sysMessageMapper.selectCount(allWrapper);

        LambdaQueryWrapper<SysMessage> systemWrapper = new LambdaQueryWrapper<>();
        systemWrapper.eq(SysMessage::getUserId, userId).eq(SysMessage::getIsRead, 0).eq(SysMessage::getType, "system");
        long systemCount = sysMessageMapper.selectCount(systemWrapper);

        LambdaQueryWrapper<SysMessage> businessWrapper = new LambdaQueryWrapper<>();
        businessWrapper.eq(SysMessage::getUserId, userId).eq(SysMessage::getIsRead, 0).eq(SysMessage::getType, "business");
        long businessCount = sysMessageMapper.selectCount(businessWrapper);

        result.put("all", allCount);
        result.put("system", systemCount);
        result.put("business", businessCount);
        return result;
    }

    /**
     * 批量标记已读
     */
    @Transactional
    public void markAsRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        LambdaUpdateWrapper<SysMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(SysMessage::getId, ids)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getUpdateTime, LocalDateTime.now());
        sysMessageMapper.update(null, wrapper);
        log.info("标记消息已读, ids={}", ids);
    }

    /**
     * 批量删除消息
     */
    @Transactional
    public void deleteMessages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        sysMessageMapper.deleteBatchIds(ids);
        log.info("删除消息, ids={}", ids);
    }

    /**
     * 发送消息（供其他业务模块调用）
     */
    public void sendMessage(Long userId, String type, String title, String summary, String content,
                            String actionUrl, String actionTitle, String actionDesc) {
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setType(type);
        msg.setTitle(title);
        msg.setSummary(summary);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setActionUrl(actionUrl);
        msg.setActionTitle(actionTitle);
        msg.setActionDesc(actionDesc);
        msg.setCreateTime(LocalDateTime.now());
        msg.setUpdateTime(LocalDateTime.now());
        sysMessageMapper.insert(msg);
        log.info("发送消息, userId={}, title={}", userId, title);
    }

    /**
     * 向企业所有员工发送消息（系统通知：上链成功等场景）
     * 查询 sys_user 中 enterprise_id = enterpriseId 且 status = 1 的所有用户
     */
    public void sendToEnterprise(Long enterpriseId, String type, String title, String summary,
                                  String content, String actionUrl, String actionTitle, String actionDesc) {
        if (enterpriseId == null) {
            log.warn("sendToEnterprise: enterpriseId is null, skip");
            return;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEnterpriseId, enterpriseId)
               .eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        if (users.isEmpty()) {
            log.warn("sendToEnterprise: no users found for enterpriseId={}", enterpriseId);
            return;
        }
        for (SysUser user : users) {
            sendMessage(user.getId(), type, title, summary, content, actionUrl, actionTitle, actionDesc);
        }
        log.info("sendToEnterprise: sent {} messages for enterpriseId={}, title={}", users.size(), enterpriseId, title);
    }

    /**
     * 鍚戞墍鏈夊惎鐢ㄧ殑骞冲彴绠＄悊鍛樺彂閫佹秷鎭?
     */
    public void sendToAdmins(String type, String title, String summary, String content,
                             String actionUrl, String actionTitle, String actionDesc) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserType, 1)
                .eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        if (users == null || users.isEmpty()) {
            log.warn("sendToAdmins: no active admin users found");
            return;
        }
        int sentCount = 0;
        for (SysUser user : users) {
            if (user == null || user.getId() == null || user.getUserType() == null || user.getStatus() == null) {
                continue;
            }
            if (user.getUserType() != 1 || user.getStatus() != 1) {
                continue;
            }
            sendMessage(user.getId(), type, title, summary, content, actionUrl, actionTitle, actionDesc);
            sentCount++;
        }
        log.info("sendToAdmins: sent {} messages, title={}", sentCount, title);
    }
}
