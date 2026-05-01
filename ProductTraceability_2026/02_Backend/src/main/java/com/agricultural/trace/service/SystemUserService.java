package com.agricultural.trace.service;

import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.SysLoginLog;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.SysLoginLogMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserService {

    public static final String DEFAULT_RESET_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public Page<SysUser> getUserList(Integer current, Integer size, String username,
                                     String phone, String keyword, Integer userType, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.like(SysUser::getPhone, phone);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size), wrapper);
        enrichEnterpriseNames(page.getRecords());
        enrichLastLoginTimes(page.getRecords());
        return page;
    }

    @Transactional
    public void addUser(SysUser user, String password, List<Long> roleIds) {
        LambdaQueryWrapper<SysUser> check = new LambdaQueryWrapper<>();
        check.eq(SysUser::getUsername, user.getUsername());
        if (sysUserMapper.selectCount(check) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        applyEnterpriseBinding(user);

        String pwd = StringUtils.hasText(password) ? password : DEFAULT_RESET_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        log.info("新增用户成功, username={}", user.getUsername());
    }

    @Transactional
    public void updateUser(SysUser user, List<Long> roleIds) {
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getRealName() != null) {
            existing.setRealName(user.getRealName());
        }
        if (user.getPhone() != null) {
            existing.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            existing.setEmail(user.getEmail());
        }
        if (user.getAvatar() != null) {
            existing.setAvatar(user.getAvatar());
        }
        if (user.getUserType() != null) {
            existing.setUserType(user.getUserType());
        }
        existing.setEnterpriseId(user.getEnterpriseId());
        if (user.getRemark() != null) {
            existing.setRemark(user.getRemark());
        }

        boolean shouldClearEnterpriseBinding = !Integer.valueOf(2).equals(existing.getUserType());
        applyEnterpriseBinding(existing);

        existing.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        if (shouldClearEnterpriseBinding) {
            clearEnterpriseBinding(existing.getId(), existing.getUserType());
            existing.setEnterpriseId(null);
            existing.setEnterpriseType(null);
        }
        log.info("更新用户成功, id={}", user.getId());
    }

    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getId() == 1L) {
            throw new RuntimeException("不能删除超级管理员");
        }
        sysUserMapper.deleteById(id);
        log.info("删除用户成功, id={}", id);
    }

    public String resetPassword(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("重置密码成功, id={}", id);
        return DEFAULT_RESET_PASSWORD;
    }

    public void toggleStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getId() == 1L) {
            throw new RuntimeException("不能禁用超级管理员");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("用户状态变更, id={}, status={}", id, status);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("用户修改密码成功, id={}", userId);
    }

    private void applyEnterpriseBinding(SysUser user) {
        if (!Integer.valueOf(2).equals(user.getUserType())) {
            user.setEnterpriseId(null);
            user.setEnterpriseType(null);
            return;
        }

        if (user.getEnterpriseId() == null) {
            throw new RuntimeException("企业用户必须绑定所属企业");
        }

        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        if (enterprise == null) {
            throw new RuntimeException("关联企业不存在");
        }
        user.setEnterpriseType(enterprise.getEnterpriseType());
    }

    private void enrichEnterpriseNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> enterpriseIds = users.stream()
                .map(SysUser::getEnterpriseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (enterpriseIds.isEmpty()) {
            return;
        }

        List<EnterpriseInfo> enterprises = enterpriseInfoMapper.selectBatchIds(enterpriseIds);
        Map<Long, EnterpriseInfo> enterpriseMap = (enterprises == null ? Collections.<EnterpriseInfo>emptyList() : enterprises).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(EnterpriseInfo::getId, Function.identity(), (left, right) -> left));

        for (SysUser user : users) {
            if (user.getEnterpriseId() == null) {
                continue;
            }

            EnterpriseInfo enterprise = enterpriseMap.get(user.getEnterpriseId());
            if (enterprise != null && !Integer.valueOf(1).equals(enterprise.getAuditStatus())) {
                user.setUserType(3);
                user.setEnterpriseId(null);
                user.setEnterpriseType(null);
                user.setEnterpriseName(null);
                clearEnterpriseBinding(user.getId(), 3);
                continue;
            }

            user.setEnterpriseName(enterprise == null ? null : enterprise.getEnterpriseName());
        }
    }

    private void enrichLastLoginTimes(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream()
                .map(SysUser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> usernames = users.stream()
                .map(SysUser::getUsername)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty() && usernames.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysLoginLog::getUserId, SysLoginLog::getUsername, SysLoginLog::getLoginTime)
                .eq(SysLoginLog::getStatus, 1);
        if (!userIds.isEmpty() && !usernames.isEmpty()) {
            wrapper.and(w -> w.in(SysLoginLog::getUserId, userIds)
                    .or().in(SysLoginLog::getUsername, usernames));
        } else if (!userIds.isEmpty()) {
            wrapper.in(SysLoginLog::getUserId, userIds);
        } else {
            wrapper.in(SysLoginLog::getUsername, usernames);
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);

        List<SysLoginLog> logs = sysLoginLogMapper.selectList(wrapper);
        if (logs == null || logs.isEmpty()) {
            return;
        }

        Map<Long, LocalDateTime> latestByUserId = new HashMap<>();
        Map<String, LocalDateTime> latestByUsername = new HashMap<>();
        for (SysLoginLog log : logs) {
            if (log.getUserId() != null && !latestByUserId.containsKey(log.getUserId())) {
                latestByUserId.put(log.getUserId(), log.getLoginTime());
            }
            if (StringUtils.hasText(log.getUsername()) && !latestByUsername.containsKey(log.getUsername())) {
                latestByUsername.put(log.getUsername(), log.getLoginTime());
            }
        }

        for (SysUser user : users) {
            LocalDateTime lastLoginTime = latestByUserId.get(user.getId());
            if (lastLoginTime == null && StringUtils.hasText(user.getUsername())) {
                lastLoginTime = latestByUsername.get(user.getUsername());
            }
            user.setLastLoginTime(lastLoginTime);
        }
    }

    private void clearEnterpriseBinding(Long userId, Integer userType) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getUserType, userType)
                .set(SysUser::getEnterpriseId, null)
                .set(SysUser::getEnterpriseType, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        sysUserMapper.update(null, wrapper);
    }
}
