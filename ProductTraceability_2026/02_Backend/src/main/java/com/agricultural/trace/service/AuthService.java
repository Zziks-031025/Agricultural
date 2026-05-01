package com.agricultural.trace.service;

import com.agricultural.trace.dto.LoginDTO;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.agricultural.trace.utils.JwtUtils;
import com.agricultural.trace.vo.LoginVO;
import com.agricultural.trace.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private EnterpriseInfoMapper enterpriseInfoMapper;

    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        wrapper.eq(SysUser::getStatus, 1);

        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        validateUserAccess(user);

        boolean matches;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            matches = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        } else {
            matches = loginDTO.getPassword().equals(user.getPassword());
        }

        if (!matches) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(buildUserInfo(user));
        return loginVO;
    }

    public UserInfoVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        validateUserAccess(user);
        return buildUserInfo(user);
    }

    public void registerOrdinaryUser(Map<String, Object> body) {
        String username = readText(body, "username");
        String password = readText(body, "password");
        String confirmPassword = readText(body, "confirmPassword");
        String realName = readText(body, "realName");
        String phone = readText(body, "phone");

        if (!StringUtils.hasText(username) || username.trim().length() < 3) {
            throw new RuntimeException("用户名至少3位");
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new RuntimeException("密码至少6位");
        }
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次密码输入不一致");
        }
        if (!StringUtils.hasText(realName)) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("请输入正确的11位手机号");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username.trim());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName.trim());
        user.setPhone(phone.trim());
        user.setUserType(3);
        user.setEnterpriseId(null);
        user.setEnterpriseType(null);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
    }

    private UserInfoVO buildUserInfo(SysUser user) {
        EnterpriseInfo enterprise = resolveEnterpriseContext(user);
        if (enterprise != null
                && !Integer.valueOf(1).equals(enterprise.getAuditStatus())
                && Integer.valueOf(2).equals(user.getUserType())) {
            downgradeLegacyApplicantAccount(user);
        }

        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setUserType(user.getUserType());

        if (enterprise != null) {
            userInfo.setEnterpriseId(enterprise.getId());
            userInfo.setEnterpriseType(enterprise.getEnterpriseType());
            userInfo.setEnterpriseName(enterprise.getEnterpriseName());
            userInfo.setEnterpriseAuditStatus(enterprise.getAuditStatus());
            userInfo.setAuditRemark(enterprise.getAuditRemark());
            userInfo.setEnterpriseStatus(enterprise.getStatus());

            if (Integer.valueOf(2).equals(user.getUserType()) && user.getEnterpriseType() == null) {
                user.setEnterpriseType(enterprise.getEnterpriseType());
                sysUserMapper.updateById(user);
            }
        } else {
            userInfo.setEnterpriseId(user.getEnterpriseId());
        }

        List<String> roles = new ArrayList<>();
        if (user.getUserType() == 1) {
            roles.add("admin");
        } else if (user.getUserType() == 2) {
            roles.add("enterprise");
        } else {
            roles.add("user");
        }
        userInfo.setRoles(roles);

        List<String> permissions = new ArrayList<>();
        permissions.add("*:*:*");
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    private EnterpriseInfo resolveEnterpriseContext(SysUser user) {
        EnterpriseInfo enterprise = null;
        if (user.getEnterpriseId() != null) {
            enterprise = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        }
        if (enterprise != null) {
            return enterprise;
        }

        LambdaQueryWrapper<EnterpriseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseInfo::getCreateBy, user.getId())
                .orderByDesc(EnterpriseInfo::getCreateTime)
                .last("limit 1");
        return enterpriseInfoMapper.selectOne(wrapper);
    }

    private void downgradeLegacyApplicantAccount(SysUser user) {
        user.setUserType(3);
        user.setEnterpriseId(null);
        user.setEnterpriseType(null);

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, user.getId())
                .set(SysUser::getUserType, 3)
                .set(SysUser::getEnterpriseId, null)
                .set(SysUser::getEnterpriseType, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        sysUserMapper.update(null, wrapper);
    }

    private void validateUserAccess(SysUser user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        if (user.getUserType() == null || user.getUserType() != 2) {
            return;
        }
        if (user.getEnterpriseId() == null) {
            throw new RuntimeException("企业用户未关联企业信息");
        }

        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        if (enterprise == null) {
            throw new RuntimeException("企业信息不存在，暂不允许登录");
        }
        if (enterprise.getStatus() == null || enterprise.getStatus() != 1) {
            throw new RuntimeException("企业已被禁用，暂不允许访问");
        }
    }

    private String readText(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : value.toString();
    }
}
